package com.forpets.domain.admin.controller;

import com.forpets.domain.admin.service.ReservationAdminService;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class ReservationAdminController {

    private final ReservationAdminService adminReservationService;

    /**
     * 불가피한 취소 요청 목록 조회
     */
    @GetMapping("/cancel-requests")
    public ResponseEntity<ApiResponse<List<ReservationResponseDto>>> getCancelRequests() {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.getCancelRequests()));
    }

    /**
     * 불가피한 취소 요청 승인 → 전액 환불 + CANCELED
     */
    @PatchMapping("/{reservationId}/cancel-approve")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> approveCancelRequest(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.approveCancelRequest(reservationId)));
    }

    /**
     * 불가피한 취소 요청 거절 → CONFIRMED 복원
     */
    @PatchMapping("/{reservationId}/cancel-reject")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> rejectCancelRequest(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.rejectCancelRequest(reservationId)));
    }
}
