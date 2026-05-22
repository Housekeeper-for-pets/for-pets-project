package com.forpets.domain.chat.entity;

import com.forpets.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "chat_room_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_participant_room_member",
                        columnNames = {"chat_room_id", "member_id"}
                )
        },
        indexes = {
                // 내가 참여 중인 채팅방 목록 조회
                @Index(name = "idx_chat_room_participant_member_id", columnList = "member_id"),
                // 특정 채팅방의 참여자 2명을 조회
                @Index(name = "idx_chat_room_participant_chat_room_id", columnList = "chat_room_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "is_left", nullable = false)
    private boolean isLeft =false;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "visible_from_at")
    private LocalDateTime visibleFromAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Builder
    private ChatRoomParticipant(Long chatRoomId, Long memberId){
        this.chatRoomId = chatRoomId;
        this.memberId = memberId;
        this.isLeft = false;
    }

    // 채팅방 나가기
    public void leave(LocalDateTime now) {
        this.isLeft = true;
        this.leftAt = now;
        this.visibleFromAt = now;
    }

    // 채팅방 재입장
    public void rejoin() {
        this.isLeft = false;
    }

    // 마지막 읽은 메시지 갱신
    public void updateLastRead(Long messageId, LocalDateTime now) {
        if (messageId == null) {
            return;
        }

        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
            this.lastReadAt = now;
        }
    }

    public boolean isLeft() {
        return this.isLeft;
    }
}
