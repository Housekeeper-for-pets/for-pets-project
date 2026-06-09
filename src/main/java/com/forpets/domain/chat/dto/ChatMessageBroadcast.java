package com.forpets.domain.chat.dto;

import com.forpets.domain.chat.entity.ChatMessageType;
import lombok.Builder;

import java.time.LocalDateTime;

// 구독자에게 전달할 채팅 메시지
@Builder
public record ChatMessageBroadcast(
        Long chatRoomId,
        Long messageId,
        ChatMessageType messageType,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt
) {
}
