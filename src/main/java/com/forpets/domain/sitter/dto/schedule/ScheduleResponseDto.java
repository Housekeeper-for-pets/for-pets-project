package com.forpets.domain.sitter.dto.schedule;

import com.forpets.domain.sitter.entity.SitterSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResponseDto(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
    public static ScheduleResponseDto from(SitterSchedule schedule) {
        return new ScheduleResponseDto(
                schedule.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }
}