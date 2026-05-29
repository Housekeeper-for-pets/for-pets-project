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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProposalController {
    /*
    역방향 시나리오

    1. Pet 을 등록한 Member 가 Post 를 등록
    2. Sitter Profile 을 등록한 Member (Sitter) 가 특정 Post 에 Proposal 생성
    3. Post 를 등록한 당사자가 Proposal 을 accept
    (3-1. 공고 등록 당사자는 Proposal 을 reject 할 수 있음)
    (3-2. Sitter 는 Proposal.status = PENDING 일 때 withdraw (cancel) 할 수 있음)
    4. reservation.status = PENDING 상태의 예약이 생성
        -> 양측에서 결제가 이루어짐 (reservation.status = CONFIRMED)
        -> (CONFIRMED 상태 일 때 취소 시 취소 정책을 따름)
        -> 케어 진행
        -> V2: 리뷰 등록
     */

    private final ProposalService proposalService;

    /*
    1. 공고에 제안 등록 (역방향)
     */
    @PreAuthorize("hasRole('SITTER')")
    @PostMapping("/api/posts/{postId}/proposals")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId,
            @RequestBody @Valid CreateProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(proposalService.create(currentMember.id(), postId, request)));
    }

    /*
    2. 공고 등록자가 공고에 들어온 제안 목록을 조회
     */
    @GetMapping("/api/posts/{postId}/proposals")
    public ResponseEntity<ApiResponse<List<ProposalResponseDto>>> getByPostId(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long postId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getByPostId(currentMember.id(), postId)));
    }

    /*
    3. 시터가 본인이 등록한 제안 목록을 조회
     */
    @GetMapping("/api/proposals/me")
    public ResponseEntity<ApiResponse<List<ProposalResponseDto>>> getMyProposals(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getMyProposals(currentMember.id())));
    }

    /*
    4. 제안의 상세 내용을 조회
    제안이 들어온 공고의 등록자(반려인) 또는 제안 생성자(시터)만 조회 가능
     */
    @GetMapping("/api/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> getById(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.getById(currentMember.id(), proposalId)));
    }

    /*
    5. 제안 수락
    by 공고 등록자 (반려인)
    중요!!!! 이 때 PENDING 상태의 예약이 생성 됨
     */
    @PatchMapping("/api/proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> accept(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.accept(currentMember.id(), proposalId)));
    }

    /*
    6. 제안 거절
    by 공고 등록자 (반려인)
     */
    @PatchMapping("/api/proposals/{proposalId}/reject")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> reject(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.reject(currentMember.id(), proposalId)));
    }

    /*
    7. 제안 철회 (취소)
    by 제안 등록자 (시터)
     */
    @PatchMapping("/api/proposals/{proposalId}/withdraw")
    public ResponseEntity<ApiResponse<ProposalResponseDto>> withdraw(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long proposalId) {
        return ResponseEntity.ok(
                ApiResponse.success(proposalService.withdraw(currentMember.id(), proposalId)));
    }
}