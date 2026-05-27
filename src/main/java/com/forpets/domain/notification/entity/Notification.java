package com.forpets.domain.notification.entity;

import com.forpets.global.entity.BaseEntity;
import com.forpets.global.sse.SseEventType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_noti_receiver_read",
                columnList = "receiverId, isRead")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long receiverId;

    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SseEventType type;

    @Column(nullable = false)
    private String message;

    private Long referenceId;

    private String referenceType;

    @Column(nullable = false)
    private boolean isRead = false;

    @Builder
    public Notification(Long receiverId, Long senderId,
                        SseEventType type, String message,
                        Long referenceId, String referenceType) {
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}