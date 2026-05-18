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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReservationResponseDto>>> getMyReservations(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.getMyReservations(currentMember.id())));
    }

}
