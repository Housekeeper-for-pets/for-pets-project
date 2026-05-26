package com.forpets.domain.chat.service;

import com.forpets.domain.chat.entity.ChatRoom;
import com.forpets.domain.chat.entity.ChatRoomParticipant;
import com.forpets.domain.chat.repository.ChatRoomParticipantRepository;
import com.forpets.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatRoomCreateHelper {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    // 채팅방과 참여자 2명을 하나의 새 트랜잭션에서 생성
    // 동시 요청으로 roomkey 중복이 발생, 기존 채팅방을 다시 조회
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoom createChatRoomWithParticipants(Long memberAId, Long memberBId, String roomKey) {
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .memberAId(memberAId)
                .memberBId(memberBId)
                .roomKey(roomKey)
                .build());

        chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                .chatRoomId(chatRoom.getId())
                .memberId(memberAId)
                .build());

        chatRoomParticipantRepository.save(ChatRoomParticipant.builder()
                .chatRoomId(chatRoom.getId())
                .memberId(memberBId)
                .build());

        return chatRoom;
    }
}