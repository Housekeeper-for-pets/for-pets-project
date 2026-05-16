package com.forpets.domain.proposal.controller;

import com.forpets.domain.proposal.dto.CreateProposalRequest;
import com.forpets.domain.proposal.dto.ProposalResponseDto;
import com.forpets.domain.proposal.service.ProposalService;
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
public class ProposalController {

    private final ProposalService proposalService;

    // ===== 공고 기준 API =====

    @PostMapping("/api/posts/{postId}/proposals")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId,
            @RequestBody @Valid CreateProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(proposalService.create(currentMember.id(), postId, request)));
    }

    @GetMapping("/api/posts/{postId}/proposals")
    public ResponseEntity<ApiResponse<List<ProposalResponseDto>>> getByPostId(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getByPostId(currentMember.id(), postId)));
    }

    // ===== 제안 기준 API =====

    @GetMapping("/api/proposals/me")
    public ResponseEntity<ApiResponse<List<ProposalResponseDto>>> getMyProposals(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getMyProposals(currentMember.id())));
    }

    @GetMapping("/api/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> getById(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getById(currentMember.id(), proposalId)));
    }

    @PatchMapping("/api/proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> accept(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.accept(currentMember.id(), proposalId)));
    }

    @PatchMapping("/api/proposals/{proposalId}/reject")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> reject(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.reject(currentMember.id(), proposalId)));
    }

    @PatchMapping("/api/proposals/{proposalId}/withdraw")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> withdraw(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.withdraw(currentMember.id(), proposalId)));
    }
}