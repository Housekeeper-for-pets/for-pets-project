package com.forpets.domain.sitter.controller;

import com.forpets.domain.sitter.dto.CreateSitterRequest;
import com.forpets.domain.sitter.dto.SitterResponseDto;
import com.forpets.domain.sitter.dto.UpdateSitterRequest;
import com.forpets.domain.sitter.dto.UpdateSitterStatusRequest;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sitters")
@RequiredArgsConstructor
public class SitterController {

    private final SitterService sitterService;

    @PostMapping
    public ResponseEntity<ApiResponse<SitterResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreateSitterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sitterService.create(currentMember.id(), request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SitterResponseDto>> getMyProfile(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.getMyProfile(currentMember.id())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<SitterResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateSitterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.update(currentMember.id(), request)));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<SitterResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateSitterStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.updateStatus(currentMember.id(), request)));
    }


}