package com.forpets.domain.pet.controller;

import com.forpets.domain.pet.dto.CreatePetRequest;
import com.forpets.domain.pet.dto.PetResponseDto;
import com.forpets.domain.pet.dto.UpdatePetRequest;
import com.forpets.domain.pet.service.PetService;
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
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    // 1. 반려동물 등록
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponseDto>> create(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid CreatePetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(petService.create(currentMember.id(), request)));
    }

    // 2. 내 반려동물 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetResponseDto>>> getMyPets(
            @LoginUser CurrentMember currentMember
            ) {
        return ResponseEntity.ok(ApiResponse.success(petService.getMyPets(currentMember.id())));
    }

    // 3. 반려동물 상세 조회
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponseDto>> getOne(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(petService.getById(currentMember.id(), petId)));
    }

    // 4. 반려동물 정보 수정
    // postman 에서 실험할때는 변경되지 않는 정보까지 작성해야하지만,
    // Front 를 도입하면서 변경되지 않는 정보까지 다시 입력할 필요성이 없어집니다.
    @PutMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponseDto>> update(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long petId,
            @RequestBody @Valid UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.update(currentMember.id(), petId, request)));
    }

    // 반려동물 삭제
    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser CurrentMember currentMember,
            @PathVariable Long petId) {
        petService.delete(currentMember.id(), petId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}