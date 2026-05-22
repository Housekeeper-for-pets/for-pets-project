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
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_room_room_key", columnNames = "room_key")
        },
        indexes = {
                // 특정 회원 채팅방 조회
                @Index(name = "idx_chat_room_member_a_id", columnList = "member_a_id"),
                @Index(name = "idx_chat_room_member_b_id", columnList = "member_b_id"),
                // 최신순 정렬 일이 많으니 인덱스 걸기
                @Index(name = "idx_chat_room_last_message_at",columnList = "last_message_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_a_id", nullable = false)
    private Long memberAId;

    @Column(name = "member_b_id", nullable = false)
    private Long memberBId;

    @Column(name = "room_key", nullable = false, length = 50)
    private String roomKey;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder
    private ChatRoom(Long memberAId, Long memberBId, String roomKey){
        this.memberAId = memberAId;
        this.memberBId = memberBId;
        this.roomKey = roomKey;
    }

    public void updateLastMessage(Long messageId, LocalDateTime messageAt) {
        this.lastMessageId = messageId;
        this.lastMessageAt = messageAt;
    }

    // 1:1 채팅방 키 생성
    public static String generateRoomKey(Long memberId1, Long memberId2) {
        long memberAId = Math.min(memberId1, memberId2);
        long memberBId = Math.max(memberId1, memberId2);

        return memberAId + ":" + memberBId;
    }

    // 작은 회원 ID 반환
    public static Long resolveMemberAId(Long memberId1, Long memberId2) {
        return Math.min(memberId1, memberId2);
    }

    // 큰 회원 ID 반환
    public static Long resolveMemberBId(Long memberId1, Long memberId2) {
        return Math.max(memberId1, memberId2);
    }
}
