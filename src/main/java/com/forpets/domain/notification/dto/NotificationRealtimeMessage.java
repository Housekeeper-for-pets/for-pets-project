package com.forpets.domain.notification.dto;

import com.forpets.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationRealtimeMessage(
        Long id,
        Long receiverId,
        String type,
        String message,
        Long referenceId,
        String referenceType,
        LocalDateTime createdAt
) {
    public static NotificationRealtimeMessage from(Notification notification) {
        return new NotificationRealtimeMessage(
                notification.getId(),
                notification.getReceiverId(),
                notification.getType().name(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getReferenceType(),
                notification.getCreatedAt()
        );
    }
}
