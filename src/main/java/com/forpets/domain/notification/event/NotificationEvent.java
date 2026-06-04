package com.forpets.domain.notification.event;

import com.forpets.global.sse.SseEventType;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

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

    //  Redis Streams 저장용 Map 변환
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("receiverId", String.valueOf(receiverId));
        map.put("senderId", senderId != null ? String.valueOf(senderId) : "");
        map.put("type", type.name());
        map.put("message", message);
        map.put("referenceId", referenceId != null ? String.valueOf(referenceId) : "");
        map.put("referenceType", referenceType != null ? referenceType : "");
        return map;
    }

    // Redis Streams에서 꺼낸 Map → NotificationEvent 변환
    public static NotificationEvent fromMap(Map<String, String> map) {
        return NotificationEvent.builder()
                .receiverId(Long.valueOf(map.get("receiverId")))
                .senderId(map.get("senderId").isEmpty() ? null : Long.valueOf(map.get("senderId")))
                .type(SseEventType.valueOf(map.get("type")))
                .message(map.get("message"))
                .referenceId(map.get("referenceId").isEmpty() ? null : Long.valueOf(map.get("referenceId")))
                .referenceType(map.get("referenceType").isEmpty() ? null : map.get("referenceType"))
                .build();
    }
}