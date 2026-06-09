package com.forpets.domain.admin.controller;

import com.forpets.domain.admin.service.SitterAdminService;
import com.forpets.domain.sitter.dto.admin.AdminSitterDetailResponseDto;
import com.forpets.domain.sitter.dto.admin.AdminSitterPageResponse;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.domain.sitter.dto.admin.RejectSitterRequest;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sitters")
@PreAuthorize("hasRole('ADMIN')")
public class SitterAdminController {

    private final SitterAdminService sitterAdminService;

    // 1. 승인 대기 시터 프로필 목록 조회 (페이지네이션)
    @GetMapping
    public ResponseEntity<ApiResponse<AdminSitterPageResponse>> getPendingSitters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterAdminService.getPendingSitters(page, size)));
    }

    // 2. 시터 프로필 상세 조회 (시터 프로필 + 신청자 멤버 정보)
    @GetMapping("/{sitterProfileId}")
    public ResponseEntity<ApiResponse<AdminSitterDetailResponseDto>> getSitterDetail(
            @PathVariable Long sitterProfileId) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterAdminService.getSitterDetail(sitterProfileId)));
    }

    // 3. 시터 프로필 요청 승인
    @PostMapping("/{sitterProfileId}/approve")
    public ResponseEntity<ApiResponse<AdminSitterResponseDto>> approve(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long sitterProfileId) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterAdminService.approve(currentMember.id(), sitterProfileId)));
    }


    // 4. 시터 프로필 요청 거절
    @PostMapping("/{sitterProfileId}/reject")
    public ResponseEntity<ApiResponse<AdminSitterResponseDto>> reject(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long sitterProfileId,
            @RequestBody @Valid RejectSitterRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterAdminService.reject(currentMember.id(), sitterProfileId, request.rejectReason())));
    }

}
