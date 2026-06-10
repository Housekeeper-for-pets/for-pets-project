package com.forpets.domain.notification.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.notification.dto.NotificationRealtimeMessage;
import com.forpets.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationRealtimeMessage realtimeMessage =
                    objectMapper.readValue(message.getBody(), NotificationRealtimeMessage.class);

            notificationService.sendRealtime(realtimeMessage);
            log.debug("[NotificationRedisSubscriber] 실시간 알림 broadcast 수신: receiverId={}, notificationId={}",
                    realtimeMessage.receiverId(), realtimeMessage.id());
        } catch (Exception e) {
            log.error("[NotificationRedisSubscriber] 실시간 알림 처리 실패", e);
        }
    }
}
