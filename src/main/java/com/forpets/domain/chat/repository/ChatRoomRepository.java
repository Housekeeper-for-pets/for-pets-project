package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // roomKey로 채팅방 조회
    @Query("SELECT c FROM ChatRoom c WHERE c.roomKey = :roomKey")
    Optional<ChatRoom> findByRoomKey(@Param("roomKey") String roomKey);

    // 현재 lastMessageId보다 새로운 메시지일 때만 갱신한다.
    // Lost Update 방지: 더 오래된 메시지가 더 최신 메시지를 덮어쓰지 못하게 막는다.
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ChatRoom c
        SET c.lastMessageId = :messageId,
            c.lastMessageAt = :messageAt
        WHERE c.id = :chatRoomId
          AND (c.lastMessageId IS NULL OR c.lastMessageId < :messageId)
    """)
    int updateLastMessageIfNewer(
            @Param("chatRoomId") Long chatRoomId,
            @Param("messageId") Long messageId,
            @Param("messageAt") LocalDateTime messageAt
    );
}
