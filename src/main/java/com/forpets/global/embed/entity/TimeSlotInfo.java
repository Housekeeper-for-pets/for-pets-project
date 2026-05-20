package com.forpets.global.embed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSlotInfo {

    @Column(nullable = false)
    private LocalDate careDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer sequence;

    public static TimeSlotInfo of(LocalDate careDate, LocalTime startTime,
                                  LocalTime endTime, Integer sequence) {
        TimeSlotInfo info = new TimeSlotInfo();
        info.careDate = careDate;
        info.startTime = startTime;
        info.endTime = endTime;
        info.sequence = sequence;
        return info;
    }
}