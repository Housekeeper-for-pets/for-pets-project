package com.forpets.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ChatRoomLeaveResponse(
        Long chatRoomId,

        @JsonProperty("isLeft")
        boolean isLeft,

        LocalDateTime leftAt,
        LocalDateTime visibleFromAt
) {
}
