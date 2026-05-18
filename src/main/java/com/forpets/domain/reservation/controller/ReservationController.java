package com.forpets.domain.reservation.controller;

import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> getDetail(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.getDetail(currentMember.id(), reservationId)));
    }

    /*
    예약 확정
    보호자 또는 시터가 각각 호출해서 결제 진행 (V2) MVP 에서는 호출만 하면 결제를 한걸로 침
    양쪽 다 완료되면 그 때 CONFIRMED
     */
        /*
    예약 확정 요청
    보호자 또는 시터가 각각 호출하면 해당 측 결제 확인 처리
    양쪽 다 완료되면 CONFIRMED로 전환
     */
    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> confirm(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(reservationService.confirm(currentMember.id(), reservationId)));
    }



}
