package com.forpets.global.embed.dto;

import com.forpets.global.embed.entity.TimeSlotInfo;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeSlotResponseDto(
        Long timeSlotId,
        LocalDate careDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer sequence
) {
    public static TimeSlotResponseDto of(Long timeSlotId, TimeSlotInfo info) {
        return new TimeSlotResponseDto(
                timeSlotId, info.getCareDate(), info.getStartTime(),
                info.getEndTime(), info.getSequence());
    }
}
