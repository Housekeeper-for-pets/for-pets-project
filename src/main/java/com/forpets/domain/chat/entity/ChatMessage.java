package com.forpets.domain.chat.entity;

import com.forpets.global.entity.CreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_message",
        indexes = {
                // 특정 채팅방 메세지ID 페이징 조회
                @Index(name = "idx_chat_message_chat_room_id_id", columnList = "chat_room_id, id"),
                // visibleFromAt 이후 메세지 조회
                @Index(name = "idx_chat_message_chat_room_id_created_at", columnList = "chat_room_id, created_at"),
                // 특정 사용자가 보낸 메세지 조회
                @Index(name = "idx_chat_message_sender_id", columnList = "sender_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends CreatedEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        // 메세지가 속한 채팅방ID
        @Column(name = "chat_room_id", nullable = false)
        private Long chatRoomId;

        @Column(name = "sender_id", nullable = false)
        private Long senderId;

        // 메세제 조회 시 탈퇴회원 닉네임 저장
        @Column(name = "sender_nickname", nullable = false,length = 50)
        private String senderNickname;

        @Enumerated(EnumType.STRING)
        @Column(name = "message_type", nullable = false, length = 20)
        private ChatMessageType messageType;

        @Column(nullable = false,length = 1000)
        private String content;

        @Builder
        private ChatMessage(
                Long chatRoomId,
                Long senderId,
                String senderNickname,
                ChatMessageType messageType,
                String content
        ){
            this.chatRoomId = chatRoomId;
            this.senderId = senderId;
            this.senderNickname = senderNickname;
            this.messageType = messageType;
            this.content = content;
        }
}
