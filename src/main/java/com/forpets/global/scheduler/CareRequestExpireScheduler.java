package com.forpets.global.scheduler;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.exception.CareRequestErrorCode;
import com.forpets.domain.carerequest.exception.CareRequestException;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.carerequest.service.CareRequestExpireService;
import com.forpets.domain.carerequest.service.CareRequestLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/*
스케줄러: 마지막 sequence 의 timeslot 이 지난 PENDING/ACCEPTED 돌봄 요청을 EXPIRED 로 만료 처리
기본 10분 간격 실행 (fixedDelay)

ACCEPTED 도 만료 대상에 포함하는 이유:
 - 시터가 수락한 시점에 Reservation 이 PENDING 으로 생성되고, 그 Reservation 은
   별도 흐름 (결제 만료/취소/완료) 으로 처리된다.
 - timeslot 이 지났음에도 CareRequest 가 ACCEPTED 로 남아있다면 사용자에게도 더 이상
   의미 있는 상태가 아니므로 EXPIRED 로 정리한다.

구조 (ReservationExpireScheduler 와 동일):
 1) 트랜잭션 바깥에서 후보군 조회
 2) for each list -> CareRequest Lock 획득 (key: lock:careRequest:{id})
 3) 건별 try/catch
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CareRequestExpireScheduler {

    private static final List<CareRequestStatus> EXPIRE_TARGET_STATUSES =
            List.of(CareRequestStatus.PENDING, CareRequestStatus.ACCEPTED);

    private final CareRequestRepository careRequestRepository;
    private final CareRequestLockService careRequestLockService;
    private final CareRequestExpireService careRequestExpireService;

    @Scheduled(fixedDelayString = "${care-request.expire.fixed-delay:600000}")
    public void expireCareRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        List<CareRequest> targets = careRequestRepository
                .findExpireCandidates(EXPIRE_TARGET_STATUSES, today, nowTime);

        if (targets.isEmpty()) {
            log.info("[CareRequestExpireScheduler] 만료 대상 없음");
            return;
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (CareRequest careRequest : targets) {
            Long careRequestId = careRequest.getId();
            try {
                careRequestLockService.executeWithCareRequestLock(careRequestId, () -> {
                    careRequestExpireService.expireOne(careRequestId);
                    return null;
                });
                success++;
            } catch (CareRequestException e) {
                if (e.getErrorCode() == CareRequestErrorCode.CARE_REQUEST_LOCK_FAILED) {
                    log.info("[CareRequestExpireScheduler] Lock 획득 실패, 다음 주기 재시도 careRequestId={}",
                            careRequestId);
                    skipped++;
                } else {
                    log.error("[CareRequestExpireScheduler] 만료 처리 실패 careRequestId={}",
                            careRequestId, e);
                    failed++;
                }
            } catch (Exception e) {
                log.error("[CareRequestExpireScheduler] 만료 처리 실패 careRequestId={}",
                        careRequestId, e);
                failed++;
            }
        }

        log.info("[CareRequestExpireScheduler] 만료 처리 결과 대상={}, 성공={}, 스킵={}, 실패={}",
                targets.size(), success, skipped, failed);
    }
}
