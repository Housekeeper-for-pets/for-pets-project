package com.forpets.domain.chat.repository;

import com.forpets.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // roomKey로 채팅방 조회
    Optional<ChatRoom> findByRoomKey(String roomKey);
}
