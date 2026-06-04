package com.forpets.domain.notification.broker;

import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.global.config.RedisStreamConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.notifications.broker", havingValue = "redis-stream")
@RequiredArgsConstructor
public class RedisStreamNotificationBroker implements NotificationMessageBroker {

    private static final String NAME = RedisStreamNotificationBroker.class.getSimpleName();

    private final StringRedisTemplate redisTemplate;

    @Override
    public void publish(NotificationEvent event) {
        // NotificationEvent → Map<String, String> 변환 후 XADD
        MapRecord<String, String, String> record =
                MapRecord.create(RedisStreamConfig.STREAM_KEY, event.toMap());

        RecordId recordId = redisTemplate.opsForStream().add(record);

        log.info("{} => Redis Stream 발행: type={}, receiver={}, recordId={}",
                NAME, event.getType(), event.getReceiverId(), recordId);
    }
}