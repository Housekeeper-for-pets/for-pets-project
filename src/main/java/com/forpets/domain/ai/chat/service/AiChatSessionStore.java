package com.forpets.domain.ai.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiChatSessionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatSessionStore {

    private static final String KEY_PREFIX = "ai:chat:session:";
    private static final TypeReference<List<AiChatSessionMessage>> MESSAGE_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${forpets.ai.chat.session-ttl-minutes:30}")
    private long sessionTtlMinutes;

    @Value("${forpets.ai.chat.context-turns:5}")
    private int contextTurns;

    public String resolveSessionId(String requestedSessionId) {
        if (StringUtils.hasText(requestedSessionId)) {
            return requestedSessionId;
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 사용자별 Redis key를 분리하여 같은 sessionId가 전달되어도 다른 사용자의 대화가 섞이지 않게 합니다.
     */
    public List<AiChatSessionMessage> loadRecentMessages(Long memberId, String sessionId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(memberId, sessionId));
            if (!StringUtils.hasText(json)) {
                return List.of();
            }
            List<AiChatSessionMessage> messages = objectMapper.readValue(json, MESSAGE_LIST_TYPE);
            return trimMessages(messages);
        } catch (Exception exception) {
            log.warn("AI 챗봇 세션 조회 실패. 빈 대화 이력으로 진행합니다. memberId={}, sessionId={}",
                    memberId, sessionId, exception);
            return List.of();
        }
    }

    public void appendTurn(Long memberId, String sessionId, String userMessage, String assistantAnswer) {
        try {
            List<AiChatSessionMessage> messages = new ArrayList<>(loadRecentMessages(memberId, sessionId));
            messages.add(AiChatSessionMessage.user(userMessage));
            messages.add(AiChatSessionMessage.assistant(assistantAnswer));

            stringRedisTemplate.opsForValue().set(
                    key(memberId, sessionId),
                    objectMapper.writeValueAsString(trimMessages(messages)),
                    Duration.ofMinutes(sessionTtlMinutes)
            );
        } catch (JsonProcessingException exception) {
            log.warn("AI 챗봇 세션 직렬화 실패. 응답은 정상 반환합니다. memberId={}, sessionId={}",
                    memberId, sessionId, exception);
        } catch (Exception exception) {
            log.warn("AI 챗봇 세션 저장 실패. 응답은 정상 반환합니다. memberId={}, sessionId={}",
                    memberId, sessionId, exception);
        }
    }

    private List<AiChatSessionMessage> trimMessages(List<AiChatSessionMessage> messages) {
        int maxMessages = Math.max(1, contextTurns) * 2;
        if (messages.size() <= maxMessages) {
            return messages;
        }
        return List.copyOf(messages.subList(messages.size() - maxMessages, messages.size()));
    }

    private String key(Long memberId, String sessionId) {
        return KEY_PREFIX + memberId + ":" + sessionId;
    }
}
