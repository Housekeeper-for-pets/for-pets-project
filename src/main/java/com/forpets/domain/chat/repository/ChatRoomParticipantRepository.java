package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
