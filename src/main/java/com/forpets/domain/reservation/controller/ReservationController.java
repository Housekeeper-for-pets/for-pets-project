package com.forpets.domain.reservation.controller;

import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;

    /*
    예약 생성은 api 로 접근하는 것이 아닌
    Proposal 을 accept 하거나, CareRequest 를 accept 했을 때 동기적으로 실행됩니다!

    Service 코드에서만 예약 생성 로직을 확인할 수 있어요 (2개 따로 작성)
     */

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReservationResponseDto>>> getMyReservations(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.getMyReservations(currentMember.id())));
    }



}
