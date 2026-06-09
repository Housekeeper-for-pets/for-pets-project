package com.forpets.domain.chat.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.chat.dto.ChatMessageBroadcast;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private static final String TOPIC_PREFIX = "chat-room:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long chatRoomId, ChatMessageBroadcast broadcast) {
        try {
            String payload = objectMapper.writeValueAsString(broadcast);
            stringRedisTemplate.convertAndSend(TOPIC_PREFIX + chatRoomId, payload);
            log.debug("[ChatRedis] PUBLISH topic=chat-room:{}, messageId={}", chatRoomId, broadcast.messageId());
        } catch (JsonProcessingException e) {
            log.error("[ChatRedis] 메시지 직렬화 실패: chatRoomId={}", chatRoomId, e);
        }
    }
}