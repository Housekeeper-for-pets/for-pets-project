package com.forpets.domain.notification.broker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.notifications.broker", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaNotificationConsumer {

    private static final String NAME = KafkaNotificationConsumer.class.getSimpleName();
    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${spring.notifications.kafka.topic:notification-events}",
            groupId = "${spring.notifications.kafka.consumer-group:notification-service}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("{} => Kafka 메시지 수신: topic={}, partition={}, offset={}, key={}",
                NAME, record.topic(), record.partition(), record.offset(), record.key());

        try {
            Map<String, String> payload = objectMapper.readValue(record.value(), PAYLOAD_TYPE);
            NotificationEvent event = NotificationEvent.fromMap(payload);

            notificationService.notify(
                    event.getReceiverId(),
                    event.getSenderId(),
                    event.getType(),
                    event.getMessage(),
                    event.getReferenceId(),
                    event.getReferenceType()
            );

            log.info("{} => Kafka 메시지 처리 완료: type={}, receiver={}",
                    NAME, event.getType(), event.getReceiverId());
        } catch (Exception e) {
            log.error("{} => Kafka 메시지 처리 실패: topic={}, partition={}, offset={}",
                    NAME, record.topic(), record.partition(), record.offset(), e);
            throw new IllegalStateException("Kafka 알림 메시지 처리에 실패했습니다.", e);
        }
    }
}
