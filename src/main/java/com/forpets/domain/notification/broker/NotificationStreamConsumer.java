package com.forpets.domain.notification.broker;

import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>> {

    private static final String NAME = NotificationStreamConsumer.class.getSimpleName();

    private final NotificationService notificationService;

    /**
     * 스트림에서 메시지 꺼내서 처리
     * receiveAutoAck 설정이라 처리 완료 시 자동 XACK
     */
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("{} => 메시지 수신: id={}", NAME, message.getId());

        try {
            // Map → NotificationEvent 변환
            NotificationEvent event = NotificationEvent.fromMap(message.getValue());

            // DB 저장 + SSE 발송 (기존 NotificationService 재사용)
            notificationService.notify(
                    event.getReceiverId(),
                    event.getSenderId(),
                    event.getType(),
                    event.getMessage(),
                    event.getReferenceId(),
                    event.getReferenceType()
            );

            log.info("{} => 메시지 처리 완료: type={}, receiver={}",
                    NAME, event.getType(), event.getReceiverId());

        } catch (Exception e) {
            log.error("{} => 메시지 처리 실패: id={}", NAME, message.getId(), e);
            // AutoAck라서 실패해도 ACK됨
            // 수동 ACK 방식이면 여기서 ACK 안 하면 PENDING으로 남아 재처리 가능
        }
    }
}