package com.forpets.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.forpets.domain.chat.entity.ChatMessageType;
import lombok.Builder;

import java.time.LocalDateTime;

// 채팅 메시지 목록에서 메시지 1개를 표현하는 응답 DTO다.
@Builder
public record ChatMessageItem(
        Long messageId,
        ChatMessageType messageType,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt,

        @JsonProperty("isMine")
        boolean isMine,

        @JsonProperty("isReadByOpponent")
        boolean isReadByOpponent
) {
}
