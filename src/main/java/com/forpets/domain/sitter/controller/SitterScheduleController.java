package com.forpets.domain.sitter.controller;

import com.forpets.domain.sitter.dto.schedule.ScheduleResponseDto;
import com.forpets.domain.sitter.dto.schedule.UpdateScheduleRequest;
import com.forpets.domain.sitter.service.SitterScheduleService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sitters")
@RequiredArgsConstructor
public class SitterScheduleController {

    private final SitterScheduleService sitterScheduleService;

    /*
    1. 시터 가능 시간 전체 교체 (PUT)
    프론트에서 요일별 토글/드래그로 설정한 전체 스케줄을 한 번에 저장
    기존 스케줄 전체 삭제 -> 신규 전체 삽입
     */
    @PutMapping("/me/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDto>>> putSchedules(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateScheduleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterScheduleService.replaceAll(currentMember.id(), request)));
    }

    /*
    내가 등록한 스케줄 조회 (사실상... 사용할 일이 있을까 싶긴 함)
     */
    @GetMapping("/me/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDto>>> getSchedules(
            @LoginUser CurrentMember currentMember) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterScheduleService.getSchedules(currentMember.id())));
    }
}

