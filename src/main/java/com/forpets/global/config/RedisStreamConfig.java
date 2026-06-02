package com.forpets.global.config;

import com.forpets.domain.notification.broker.NotificationStreamConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    private static final String NAME = RedisStreamConfig.class.getSimpleName();
    public static final String STREAM_KEY = "notification-stream";
    public static final String GROUP_NAME = "notification-group";
    public static final String CONSUMER_NAME = "notification-consumer-1";

    private final StringRedisTemplate redisTemplate;
    private final NotificationStreamConsumer notificationStreamConsumer;

    /**
     * Consumer Group 생성
     * 앱 시작 시 스트림과 그룹이 없으면 자동 생성
     */
    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer(
            RedisConnectionFactory connectionFactory) {

        // Consumer Group 생성 (없으면)
        createConsumerGroup();

        // Container 설정
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(1))  // 1초마다 폴링
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // Consumer 등록
        container.receiveAutoAck(
                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                notificationStreamConsumer
        );

        container.start();
        log.info("{} => Redis Stream Consumer 시작: stream={}, group={}",
                NAME, STREAM_KEY, GROUP_NAME);

        return container;
    }

    private void createConsumerGroup() {
        try {
            redisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("{} => Consumer Group 생성: {}", NAME, GROUP_NAME);
        } catch (Exception e) {
            // 이미 존재하면 무시 (정상)
            log.info("{} => Consumer Group 이미 존재: {}", NAME, GROUP_NAME);
        }
    }
}