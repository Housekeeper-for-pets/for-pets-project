package com.forpets.domain.sitter.dto.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateScheduleRequest(
        @NotNull(message = "스케줄 목록은 필수입니다")
        @Size(max = 7, message = "스케줄은 최대 7개(요일별 1개)까지 등록 가능합니다")
        List<@Valid ScheduleItemRequest> schedules
) {}
