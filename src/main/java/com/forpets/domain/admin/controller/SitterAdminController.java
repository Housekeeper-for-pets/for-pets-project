package com.forpets.domain.admin.controller;

import com.forpets.domain.admin.service.SitterAdminService;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sitters")
@PreAuthorize("hasRole('ADMIN')")
public class SitterAdminController {

    private final SitterAdminService sitterAdminService;

    // 1. 승인 대기 시터 프로필 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminSitterResponseDto>>> getPendingSitters() {
        return ResponseEntity.ok(ApiResponse.success(sitterAdminService.getPendingSitters()));
    }

    // 2. 시터 프로필 요청 승인
    @PostMapping("/{sitterProfileId}/approve")



    // 3. 시터 프로필 요청 거절
    @PostMapping("/{sitterProfileId}/reject")

}
