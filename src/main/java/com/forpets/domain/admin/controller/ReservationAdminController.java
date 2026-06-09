package com.forpets.domain.admin.controller;

import com.forpets.domain.admin.service.ReservationAdminService;
import com.forpets.domain.reservation.dto.ReservationPageResponse;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
public class ReservationAdminController {

    private final ReservationAdminService adminReservationService;

    /**
     * 불가피한 취소 요청 목록 조회 (페이지네이션)
     */
    @GetMapping("/cancel-requests")
    public ResponseEntity<ApiResponse<ReservationPageResponse>> getCancelRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.getCancelRequests(page, size)));
    }

    /**
     * 불가피한 취소 요청 승인 → 전액 환불 + CANCELED
     */
    @PostMapping("/{reservationId}/approve")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> approve(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.approve(reservationId)));
    }

    /**
     * 불가피한 취소 요청 거절 → CONFIRMED 복원
     */
    @PostMapping("/{reservationId}/reject")
    public ResponseEntity<ApiResponse<ReservationResponseDto>> reject(
            @PathVariable Long reservationId) {
        return ResponseEntity.ok(
                ApiResponse.success(adminReservationService.reject(reservationId)));
    }
}
