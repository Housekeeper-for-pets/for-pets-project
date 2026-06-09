package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.dto.ChatRoomSummary;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    // 채팅방 참여자 조회
    Optional<ChatRoomParticipant> findByChatRoomIdAndMemberId(Long chatRoomId, Long memberId);

    // 회원의 참여 목록 조회
    List<ChatRoomParticipant> findAllByMemberId(Long memberId);

    // 채팅방 참여자 목록 조회
    List<ChatRoomParticipant> findAllByChatRoomId(Long chatRoomId);

    // 나가지 않은 채팅방 조회
    @Query("""
        SELECT p FROM ChatRoomParticipant p
        WHERE p.memberId = :memberId
        AND p.isLeft = false
    """)
    List<ChatRoomParticipant> findAllActiveRoomsByMemberId(@Param("memberId") Long memberId);

    // 채팅방 목록 조회: 정렬·페이징·unreadCount 집계를 DB에서 한 번에 처리
    @Query("""
    SELECT new com.forpets.domain.chat.dto.ChatRoomSummary(
        r.id,
        r.memberAId,
        r.memberBId,
        r.lastMessageId,
        r.lastMessageAt,
        p.lastReadMessageId,
        p.visibleFromAt,
        (SELECT COUNT(m) FROM ChatMessage m
         WHERE m.chatRoomId = r.id
           AND m.senderId != :memberId
           AND (p.lastReadMessageId IS NULL OR m.id > p.lastReadMessageId)
           AND (p.visibleFromAt IS NULL OR m.createdAt > p.visibleFromAt))
    )
    FROM ChatRoomParticipant p
    JOIN ChatRoom r ON r.id = p.chatRoomId
    WHERE p.memberId = :memberId
      AND p.isLeft = false
      AND r.lastMessageAt IS NOT NULL
      AND (p.visibleFromAt IS NULL OR r.lastMessageAt > p.visibleFromAt)
      AND (
          :cursorLastMessageAt IS NULL OR :cursorChatRoomId IS NULL
          OR r.lastMessageAt < :cursorLastMessageAt
          OR (r.lastMessageAt = :cursorLastMessageAt AND r.id < :cursorChatRoomId)
      )
    ORDER BY r.lastMessageAt DESC, r.id DESC
""")
    List<ChatRoomSummary> findChatRoomSummaries(
            @Param("memberId") Long memberId,
            @Param("cursorLastMessageAt") LocalDateTime cursorLastMessageAt,
            @Param("cursorChatRoomId") Long cursorChatRoomId,
            Pageable pageable
    );
}
