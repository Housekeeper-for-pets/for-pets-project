package com.forpets.global.scheduler;

import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.service.ReservationExpireService;
import com.forpets.domain.reservation.service.ReservationLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/*
스케줄러: 생성 후 2시간 초과한 PENDING 예약을 만료 처리
1분마다 실행됨

구조:
 1) 트랜잭션 바깥에서 PENDING 후보군 (list) 을 조회 (이 시점은 후보일 뿐 찾고 다시 확인했을 때 pending 이 아니면 return 함)
 2) for each list -> Reservation Lock 획득 (key: lock:reservation:{id})
       성공: ReservationExpireService.expireOne(id) 호출 (이 안에서 @Transactional + 상태 재검증)
       실패: skip (결제 confirm 등 다른 요청이 진행 중 -> 다음 주기에 재시도)
 3) 건별 try/catch -> 한 건 실패해도 나머지 건은 계속 진행

후속 처리는 ReservationExpireService 안에서 다음과 같이 처리된다:
 - reservation : PENDING -> EXPIRED
 - payment     : PAID -> REFUNDED  (refundPaidPayments)
 - payment     : READY/PENDING -> EXPIRED (expireNonPaidPayments)
 - UserCoupon  : USED -> ACTIVE
 - Proposal / CareRequest : ACCEPTED -> PENDING
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private static final long PAYMENT_EXPIRE_HOURS = 2L;

    private final ReservationRepository reservationRepository;
    private final ReservationLockService reservationLockService;
    private final ReservationExpireService reservationExpireService;

    @Scheduled(fixedDelayString = "${reservation.expire.fixed-delay:60000}")
    public void expirePendingReservations() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(PAYMENT_EXPIRE_HOURS);
        List<Reservation> targets = reservationRepository
                .findAllByStatusAndCreatedAtBefore(ReservationStatus.PENDING, deadline);

        if (targets.isEmpty()) {
            log.info("[ReservationExpireScheduler] 만료 대상 없음");
            return;
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Reservation reservation : targets) {
            Long reservationId = reservation.getId();
            try {
                reservationLockService.executeWithReservationLock(reservationId, () -> {
                    reservationExpireService.expireOne(reservationId);
                    return null;
                });
                success++;
            } catch (ReservationException e) {
                if (e.getErrorCode() == ReservationErrorCode.RESERVATION_LOCK_FAILED) {
                    // 다른 요청(결제 confirm, cancel 등)이 처리 중 -> 양보, 다음 주기 재시도
                    log.info("[ReservationExpireScheduler] Lock 획득 실패, 다음 주기 재시도 reservationId={}",
                            reservationId);
                    skipped++;
                } else {
                    log.error("[ReservationExpireScheduler] 만료 처리 실패 reservationId={}",
                            reservationId, e);
                    failed++;
                }
            } catch (Exception e) {
                log.error("[ReservationExpireScheduler] 만료 처리 실패 reservationId={}",
                        reservationId, e);
                failed++;
            }
        }

        log.info("[ReservationExpireScheduler] 만료 처리 결과 대상={}, 성공={}, 스킵={}, 실패={}",
                targets.size(), success, skipped, failed);
    }
}
