package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 안읽은 메세지 개수
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.chatRoomId = :chatRoomId
        AND m.senderId != :memberId
        AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)
        AND (:visibleFromAt IS NULL OR m.createdAt > :visibleFromAt)
    """)
    long countUnread(
            @Param("chatRoomId") Long chatRoomId,
            @Param("memberId") Long memberId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("visibleFromAt") LocalDateTime visibleFromAt
    );

}
