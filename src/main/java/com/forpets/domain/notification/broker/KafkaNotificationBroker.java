package com.forpets.domain.notification.broker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.notifications.broker", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaNotificationBroker implements NotificationMessageBroker {

    private static final String NAME = KafkaNotificationBroker.class.getSimpleName();

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.notifications.kafka.topic:notification-events}")
    private String topic;

    @Override
    public void publish(NotificationEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishToKafka(event);
                }
            });
            return;
        }

        publishToKafka(event);
    }

    private void publishToKafka(NotificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event.toMap());
            String key = String.valueOf(event.getReceiverId());

            kafkaTemplate.send(topic, key, payload);
            log.info("{} => Kafka 알림 발행: topic={}, key={}, type={}, receiver={}",
                    NAME, topic, key, event.getType(), event.getReceiverId());
        } catch (JsonProcessingException e) {
            log.error("{} => Kafka 알림 payload 직렬화 실패: type={}, receiver={}",
                    NAME, event.getType(), event.getReceiverId(), e);
            throw new IllegalArgumentException("Kafka 알림 payload 직렬화에 실패했습니다.", e);
        }
    }
}
