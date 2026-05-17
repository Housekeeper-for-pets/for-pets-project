package com.forpets.domain.carerequest.controller;

import com.forpets.domain.carerequest.dto.CareRequestResponseDto;
import com.forpets.domain.carerequest.dto.CreateCareRequestDto;
import com.forpets.domain.carerequest.service.CareRequestService;
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
public class CareRequestController {

    private final CareRequestService careRequestService;

    /*
    1. 보호자가 시터에게 돌봄 요청을 생성
    프론트에서 만들 때 시터 프로필에 요청 등록하기 버튼을 누르면 자동으로 Sitter Id 가 들어갈 수 있도록 하는게 좋을듯
     */
    @PostMapping("/api/sitters/{sitterId}/requests")
    public ResponseEntity<ApiResponse<CareRequestResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long sitterId,
            @RequestBody @Valid CreateCareRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        careRequestService.create(currentMember.id(), sitterId, request)));
    }

    /*
    2. 보낸 요청 목록 조회
    모든 멤버가 조회 버튼을 볼 수 있음
    보낸 요청 목록이 없으면 [] 보내기
     */
    @GetMapping("/api/requests/sent")
    public ResponseEntity<ApiResponse<List<CareRequestResponseDto>>> getSentRequests(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.getSentRequests(currentMember.id())));
    }

    /*
    3. 요청 상세 조회
    보낸 사람 (반려인) 또는 받은 사람 (대상 시터) 만 조회 가능
     */
    @GetMapping("/api/requests/{requestId}")
    public ResponseEntity<ApiResponse<CareRequestResponseDto>> getDetail(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.getDetail(currentMember.id(), requestId)));
    }

    /*
    4. 보낸 요청 취소 (철회)
     */
    @PatchMapping("/api/requests/sent/{requestId}/cancel")
    public ResponseEntity<ApiResponse<CareRequestResponseDto>> cancel(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.cancel(currentMember.id(), requestId)));
    }

    /*
    5. 받은 돌봄 요청 목록 조회
    버튼은 뜨는데 sitter 가 아닌 사람이 누르면 시터 프로필을 등록하고 요청을 받아보세요! 가 뜨도록 하기
     */
    @GetMapping("/api/requests/received")
    public ResponseEntity<ApiResponse<List<CareRequestResponseDto>>> getReceivedRequests(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.getReceivedRequests(currentMember.id())));
    }


    /*
    6. 돌봄 요청 수락
     */
    @PatchMapping("/api/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<CareRequestResponseDto>> accept(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.accept(currentMember.id(), requestId)));
    }

    /*
    7. 돌봄 요청 거절
     */
    @PatchMapping("/api/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<CareRequestResponseDto>> reject(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                ApiResponse.success(careRequestService.reject(currentMember.id(), requestId)));
    }
}