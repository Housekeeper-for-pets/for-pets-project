package com.forpets.domain.chat.dto;

import com.forpets.domain.chat.entity.ChatMessageType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatRoomListItem(
        Long chatRoomId,
        Long opponentId,
        String opponentNickname,
        String lastMessage,
        ChatMessageType lastMessageType,
        LocalDateTime lastMessageAt,
        int unreadCount
) {
}
