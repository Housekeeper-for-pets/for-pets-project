package com.forpets.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

// 채팅 메시지 목록 조회 전체 응답 DTO다.
@Builder
public record ChatMessageListResponse(
        List<ChatMessageItem> items,

        @JsonProperty("hasNext")
        boolean hasNext,

        Long nextCursorId
) {
}
