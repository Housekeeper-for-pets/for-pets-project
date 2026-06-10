package com.forpets.global.config;

import com.forpets.domain.notification.broker.NotificationStreamConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.notifications.broker", havingValue = "redis-stream")
@RequiredArgsConstructor
public class RedisStreamConfig {

    private static final String NAME = RedisStreamConfig.class.getSimpleName();
    public static final String STREAM_KEY = "notification-stream";
    public static final String GROUP_NAME = "notification-group";

    private final StringRedisTemplate redisTemplate;
    private final NotificationStreamConsumer notificationStreamConsumer;

    @Value("${spring.notifications.redis.consumer-name:${HOSTNAME:notification-consumer}}")
    private String consumerName;

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
                Consumer.from(GROUP_NAME, consumerName),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                notificationStreamConsumer
        );

        container.start();
        log.info("{} => Redis Stream Consumer 시작: stream={}, group={}, consumer={}",
                NAME, STREAM_KEY, GROUP_NAME, consumerName);

        return container;
    }

    private void createConsumerGroup() {
        try {
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            byte[] streamKey = serializer.serialize(STREAM_KEY);

            if (streamKey == null) {
                throw new IllegalStateException("Redis Stream key 직렬화 실패: " + STREAM_KEY);
            }

            String result = redisTemplate.execute((RedisCallback<String>) connection ->
                    connection.streamCommands()
                            .xGroupCreate(
                                    streamKey,
                                    GROUP_NAME,
                                    ReadOffset.from("0-0"),
                                    true
                            )
            );

            log.info("{} => Consumer Group 생성: {}, result={}", NAME, GROUP_NAME, result);

        } catch (RedisSystemException e) {
            if (isBusyGroup(e)) {
                log.info("{} => Consumer Group 이미 존재: {}", NAME, GROUP_NAME);
                return;
            }

            log.error("{} => Consumer Group 생성 실패", NAME, e);
            throw e;
        }
    }

    private boolean isBusyGroup(Throwable e) {
        Throwable current = e;

        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
