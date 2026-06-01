package com.forpets.domain.notification.event;

import com.forpets.global.sse.SseEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationEvent {

    private final Long receiverId;
    private final Long senderId;
    private final SseEventType type;
    private final String message;
    private final Long referenceId;
    private final String referenceType;

    public static NotificationEvent of(
            Long receiverId,
            Long senderId,
            SseEventType type,
            String message,
            Long referenceId,
            String referenceType
    ) {
        return NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
    }
}