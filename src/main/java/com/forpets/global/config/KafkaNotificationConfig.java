package com.forpets.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "spring.notifications.broker", havingValue = "kafka")
public class KafkaNotificationConfig {

    @Bean
    public NewTopic notificationTopic(
            @Value("${spring.notifications.kafka.topic:notification-events}") String topic,
            @Value("${spring.notifications.kafka.partitions:3}") int partitions,
            @Value("${spring.notifications.kafka.replicas:1}") short replicas
    ) {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
