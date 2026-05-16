package com.forpets.global.embed.dto;

import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.global.embed.entity.TimeSlotInfo;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeSlotResponseDto(
        Long id,
        LocalDate careDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer sequence
) {
    public static TimeSlotResponseDto from(PostTimeSlot slot) {
        TimeSlotInfo info = slot.getTimeSlotInfo();
        return new TimeSlotResponseDto(
                slot.getId(),
                info.getCareDate(),
                info.getStartTime(),
                info.getEndTime(),
                info.getSequence()
        );
    }
}
