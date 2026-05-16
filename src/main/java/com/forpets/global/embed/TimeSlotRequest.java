package com.forpets.global.embed;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeSlotRequest(
        @NotNull(message = "돌봄 날짜는 필수입니다")
        LocalDate careDate,

        @NotNull(message = "시작 시간은 필수입니다")
        LocalTime startTime,

        @NotNull(message = "종료 시간은 필수입니다")
        LocalTime endTime
) {}