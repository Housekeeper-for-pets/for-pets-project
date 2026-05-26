package com.forpets.domain.settlement.controller;

import com.forpets.domain.settlement.dto.SettlementResponseDto;
import com.forpets.domain.settlement.service.SettlementService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SettlementResponseDto>>> getMySettlements(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.getMySettlements(currentMember.id())));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementResponseDto>> getDetail(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long settlementId) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.getDetail(
                        currentMember.id(),
                        currentMember.role(),
                        settlementId)));
    }
}
