package com.forpets.domain.sitter.controller;

import com.forpets.domain.sitter.dto.CreateSitterRequest;
import com.forpets.domain.sitter.dto.SitterResponseDto;
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


}