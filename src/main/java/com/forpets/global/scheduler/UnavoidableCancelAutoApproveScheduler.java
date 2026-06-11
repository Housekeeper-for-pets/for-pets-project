package com.forpets.global.scheduler;

import com.forpets.domain.admin.service.ReservationAdminService;
import com.forpets.domain.reservation.entity.CancelCategory;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.service.ReservationLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
스케줄러: UNAVOIDABLE 사유 취소 요청 24시간 경과 시 자동 승인

정책
 - "요청 시점 + 24시간 이 지난 그 다음 00:00 시점" 에 일괄 자동 승인
   예: 1일 22:00 요청 -> 1일 22:00 + 24h = 2일 22:00 경과 -> 그 이후 첫 00:00 = 3일 00:00 자동 승인
 - 자동 승인은 관리자 미응답에 대한 fallback. 관리자 책임이므로 별도 자동-거절 분기는 두지 않음.
 - 로그/알림 문구는 관리자 승인과 동일 (ReservationAdminService.approve 재사용)

cutoff 계산
 - 실행 시각의 오늘 00:00 에서 24시간 뺀 시각 (= 어제 00:00)
 - cancelRequestedAt <= cutoff 인 요청이 자동 승인 대상
 - 정확성 검증:
   * 1일 22:00 요청, 2일 00:00 실행 -> cutoff = 1일 00:00 -> 22:00 > 00:00 -> 미대상 (정상)
   * 1일 22:00 요청, 3일 00:00 실행 -> cutoff = 2일 00:00 -> 22:00 < 2일 00:00 -> 대상 (정상)

구조 (ReservationExpireScheduler 와 동일 패턴)
 1) 트랜잭션 바깥에서 후보군 조회
 2) Reservation Lock 획득 (key: lock:reservation:{id})
 3) ReservationAdminService.approve() 호출 - 트랜잭션 안에서 상태 재검증 (validateCancelRequested)
 4) 건별 try/catch -> 한 건 실패해도 나머지 진행

TODO: 테스트가 끝나면 @Scheduled(cron = "0 0 0 * * *") 로 되돌리고 initialDelay 라인 제거
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnavoidableCancelAutoApproveScheduler {

    private static final long AUTO_APPROVE_HOURS = 24L;

    private final ReservationRepository reservationRepository;
    private final ReservationLockService reservationLockService;
    private final ReservationAdminService reservationAdminService;

    /*
    [운영용] 매일 00:00 일괄 자동 승인 — 테스트 중에는 비활성화
     */
    // @Scheduled(cron = "0 0 0 * * *")
    /*
    [테스트용] 서버 재시작 후 20분 뒤 1회 실행
    fixedDelay 는 24시간으로 설정 — 실수로 켜둬도 하루 1회 이상 돌지 않음
     */
    @Scheduled(initialDelay = 20 * 60 * 1000L, fixedDelay = 24 * 60 * 60 * 1000L)
    @SchedulerLock(
            name = "UnavoidableCancelAutoApproveScheduler",
            lockAtMostFor = "PT30M",   // 알림 발송 포함 — 혹시 모를 긴 처리 대비
            lockAtLeastFor = "PT1H"    // N대가 동시에 00:00 트리거해도 첫 인스턴스만 처리하도록
    )
    public void autoApproveUnavoidableCancelRequests() {
        LocalDateTime cutoff = LocalDate.now().atStartOfDay().minusHours(AUTO_APPROVE_HOURS);

        List<Reservation> targets = reservationRepository
                .findAllByStatusAndCancelCategoryAndCancelRequestedAtLessThanEqual(
                        ReservationStatus.CANCEL_REQUESTED,
                        CancelCategory.UNAVOIDABLE,
                        cutoff);

        if (targets.isEmpty()) {
            log.info("[UnavoidableCancelAutoApproveScheduler] 자동 승인 대상 없음 cutoff={}", cutoff);
            return;
        }

        log.info("[UnavoidableCancelAutoApproveScheduler] 자동 승인 시작 cutoff={}, 대상={}",
                cutoff, targets.size());

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Reservation reservation : targets) {
            Long reservationId = reservation.getId();
            try {
                reservationLockService.executeWithReservationLock(reservationId, () -> {
                    reservationAdminService.approve(reservationId);
                    return null;
                });
                log.info("[UnavoidableCancelAutoApproveScheduler] 자동 승인 완료 reservationId={}, cancelRequestedAt={}",
                        reservationId, reservation.getCancelRequestedAt());
                success++;
            } catch (ReservationException e) {
                if (e.getErrorCode() == ReservationErrorCode.RESERVATION_LOCK_FAILED) {
                    log.info("[UnavoidableCancelAutoApproveScheduler] Lock 획득 실패, 다음 주기 재시도 reservationId={}",
                            reservationId);
                    skipped++;
                } else if (e.getErrorCode() == ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION) {
                    // 후보 조회 ~ 락 획득 사이에 관리자가 직접 처리 (승인/거절) 한 경우
                    log.info("[UnavoidableCancelAutoApproveScheduler] 이미 다른 경로로 처리됨 -> skip reservationId={}",
                            reservationId);
                    skipped++;
                } else {
                    log.error("[UnavoidableCancelAutoApproveScheduler] 자동 승인 실패 reservationId={}",
                            reservationId, e);
                    failed++;
                }
            } catch (Exception e) {
                log.error("[UnavoidableCancelAutoApproveScheduler] 자동 승인 실패 reservationId={}",
                        reservationId, e);
                failed++;
            }
        }

        log.info("[UnavoidableCancelAutoApproveScheduler] 자동 승인 결과 대상={}, 성공={}, 스킵={}, 실패={}",
                targets.size(), success, skipped, failed);
    }
}
