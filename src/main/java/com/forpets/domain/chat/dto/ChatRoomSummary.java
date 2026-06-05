package com.forpets.domain.chat.dto;

import java.time.LocalDateTime;

// 채팅방 목록 조회 전용 쿼리 결과를 담는 Projection DTO
public record ChatRoomSummary(
        Long chatRoomId,
        Long memberAId,
        Long memberBId,
        Long lastMessageId,
        LocalDateTime lastMessageAt,
        Long lastReadMessageId,
        LocalDateTime visibleFromAt,
        long unreadCount
) {}
