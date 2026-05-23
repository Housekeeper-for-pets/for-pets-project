package com.forpets.global.scheduler;

import com.forpets.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpireScheduler {

    private final ReservationService reservationService;

    /*
        1분마다 실행시켜서 2시간 초과한 예약을 만료시킴
        resrvation service 내부의 expirePendingReservations 얘가 @Transactional 이므로
        후속처리들이 원자적으로 실행된다

        후속 처리
        - reservation : PENDING -> EXPIRED
        - payment : (한 쪽이 결제를 했다면) PAID -> REFUNDED
        - payment : (결제 요청만 하고 결제 아직 안 했다면) READY/PENDING -> EXPIRED
        - ReservationPayment : true -> false
        - UserCoupon : USED -> ACTIVE
        - (source=)Proposal : ACCEPTED -> PENDING
        - (source=)CareRequest : ACCEPTED -> PENDING

        엄청 많네요 ......
     */

    @Scheduled(fixedDelayString = "${reservation.expire.fixed-delay:60000}")
    public void expirePendingReservations() {
        try{
            int expiredCount = reservationService.expirePendingReservations(LocalDateTime.now());
            if (expiredCount > 0) {
                log.info("[ReservationExpireScheduler] 만료 예약 처리 완료 count={}", expiredCount);
            }
        } catch (Exception e){
            log.error("[ReservationExpireScheduler] 예약 만료 처리 중 에러 발생", e);
        }
    }
}
