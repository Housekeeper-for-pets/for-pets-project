package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    // 최신 메시지 조회
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoomId = :chatRoomId
        AND (:visibleFromAt IS NULL OR m.createdAt > :visibleFromAt)
        ORDER BY m.id DESC
    """)
    List<ChatMessage> findLatestMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("visibleFromAt") LocalDateTime visibleFromAt,
            Pageable pageable
    );

    // 커서 이전 메시지 조회
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoomId = :chatRoomId
        AND m.id < :cursorId
        AND (:visibleFromAt IS NULL OR m.createdAt > :visibleFromAt)
        ORDER BY m.id DESC
    """)
    List<ChatMessage> findMessagesBeforeCursor(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursorId") Long cursorId,
            @Param("visibleFromAt") LocalDateTime visibleFromAt,
            Pageable pageable
    );
}
