package com.forpets.domain.sitter.controller;

import com.forpets.domain.sitter.dto.profile.CreateSitterRequest;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.UpdateSitterRequest;
import com.forpets.domain.sitter.dto.profile.UpdateSitterStatusRequest;
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

    /*
    1. 시터 스케줄 생성
    : MVP 에서는 자동으로 SITTER ROLE 로 자동 update
    MVP: 프로필 삭제 시 다시 등록할 수 없음
    V2: (정책 고민중)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SitterResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreateSitterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sitterService.create(currentMember.id(), request)));
    }

    /*
    2. 시터 목록 조회
    시터 스케줄이 참고용으로 수정되며 어디에 사용하면 좋을지 생각해보다가 ㅎㅎ
    목록조회 때 원하는 시간을 입력하면 비슷한 시간대를 등록한 시터를 우선적으로 추천하는 시나리오가
    생각났습니다... 완벽 일치가 아니고 포함이 아니기 때문에 번거로울 것 같긴 한데
    경안님은 뭔가 좋아하실 것 같아서 ^^.... 적어둡니닷
     */


    /*
    3. 시터 개별 조회
    시터 조회 정책이 로그인 하지 않은 유저는 목록 조회만 가능, 개별 조회는 로그인을 해야 가능
    이라고 선정되어 있습니다. 개발하며 다른 더 좋은 시나리오가 생긴다면 편하게 수정해주세요.

    다만 개별조회를 할 때 Sitter Schedule 내용을
    Sitter Dto 에 넣어서 반환해야합니다.
    Sitter Schedule 를 sitter ID 로 따로 접근하지 않고
    Dto 하나만 사용할 수 있게 정책을 수정했습니다!
     */


    // 4. 내 시터 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SitterResponseDto>> getMyProfile(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.getMyProfile(currentMember.id())));
    }

    /*
    5. 내 시터 프로필 업데이트
    Sitter profile 역시 Front 를 도입하면서 변경되지 않는 정보까지 다시 입력할 필요성이 없어짐
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<SitterResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateSitterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.update(currentMember.id(), request)));
    }

    // 6. 상태 변경 (RESERVABLE <-> NON_RESERVABLE)
    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<SitterResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateSitterStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sitterService.updateStatus(currentMember.id(), request)));
    }

    /*
    7. 시터 프로필 삭제
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser CurrentMember currentMember) {
        sitterService.delete(currentMember.id());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}