package com.forpets.domain.member.controller;

import com.forpets.domain.member.dto.*;
import com.forpets.domain.member.service.MemberService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.common.MessageResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @LoginUser CurrentMember currentMember
    ) {
        MemberResponse response = memberService.getMyInfo(currentMember.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UpdateMemberResponse>> updateMyInfo(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateMemberRequest request
    ) {
        UpdateMemberResponse response = memberService.updateMyInfo(currentMember.id(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me/password")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        memberService.changePassword(currentMember.id(), request);
        return ResponseEntity.ok(ApiResponse.success(MessageResponse.of("비밀번호가 변경되었습니다.")));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAnyRole('MEMBER', 'SITTER')")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteAccount(
            @LoginUser CurrentMember currentMember,
            HttpServletRequest request
    ) {
        memberService.deleteAccount(currentMember.id(), request);
        return ResponseEntity.ok(ApiResponse.success(MessageResponse.of("회원 탈퇴가 완료되었습니다.")));
    }
}
