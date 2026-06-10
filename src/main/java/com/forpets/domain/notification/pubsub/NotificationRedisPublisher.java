package com.forpets.domain.notification.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.notification.dto.NotificationRealtimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisPublisher {

    public static final String TOPIC = "notification-realtime";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(NotificationRealtimeMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(TOPIC, payload);
            log.info("[NotificationRedisPublisher] 실시간 알림 broadcast 발행: receiverId={}, notificationId={}",
                    message.receiverId(), message.id());
        } catch (JsonProcessingException e) {
            log.error("[NotificationRedisPublisher] 실시간 알림 직렬화 실패: receiverId={}, notificationId={}",
                    message.receiverId(), message.id(), e);
        }
    }
}
