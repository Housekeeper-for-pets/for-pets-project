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

    @Scheduled(fixedDelayString = "${reservation.expire.fixed-delay:60000}")
    public void expirePendingReservations() {
        int expiredCount = reservationService.expirePendingReservations(LocalDateTime.now());
        if (expiredCount > 0) {
            log.info("[ReservationExpireScheduler] 만료 예약 처리 완료 count={}", expiredCount);
        }
    }
}
