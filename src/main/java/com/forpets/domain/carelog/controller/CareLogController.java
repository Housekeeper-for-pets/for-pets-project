package com.forpets.domain.carelog.controller;

import com.forpets.domain.carelog.dto.CareLogResponse;
import com.forpets.domain.carelog.dto.CreateCareLogRequest;
import com.forpets.domain.carelog.service.CareLogService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CareLogController {

    private final CareLogService careLogService;

    /**
     * 케어일지 등록 (시터)
     */
    @PostMapping("/api/reservations/{reservationId}/care-logs")
    public ResponseEntity<ApiResponse<CareLogResponse>> create(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long reservationId,
            @RequestBody @Valid CreateCareLogRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        careLogService.create(currentMember.id(), reservationId, request)));
    }

    /**
     * 케어일지 목록 조회 (보호자/시터)
     */
    @GetMapping("/api/reservations/{reservationId}/care-logs")
    public ResponseEntity<ApiResponse<List<CareLogResponse>>> getByReservation(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        careLogService.getByReservation(currentMember.id(), reservationId)));
    }
}