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
    시터 가능 시간 전체 교체 (PUT)
    프론트에서 요일별 토글/드래그로 설정한 전체 스케줄을 한 번에 저장
    기존 AVAILABLE 스케줄 전체 삭제 → 신규 전체 삽입
     */
    @PutMapping("/me/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDto>>> putSchedules(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid UpdateScheduleRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterScheduleService.replaceAll(currentMember.id(), request)));
    }

    /*
    특정 시터의 가능 시간 조회
    비로그인 사용자도 조회 가능 (Public GET)
     */
    @GetMapping("/{sitterId}/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleResponseDto>>> getSchedules(
            @PathVariable Long sitterId) {
        return ResponseEntity.ok(
                ApiResponse.success(sitterScheduleService.getSchedules(sitterId)));
    }
}

