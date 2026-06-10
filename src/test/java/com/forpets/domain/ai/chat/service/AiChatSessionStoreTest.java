package com.forpets.domain.ai.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiChatSessionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AiChatSessionStoreTest {

    private AiChatSessionStore sessionStore;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        sessionStore = new AiChatSessionStore(stringRedisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(sessionStore, "sessionTtlMinutes", 30L);
        ReflectionTestUtils.setField(sessionStore, "contextTurns", 2);
    }

    @Test
    @DisplayName("[성공] sessionId가 없으면 새 세션 ID를 발급한다")
    void resolve_session_id_when_blank() {
        // when
        String sessionId = sessionStore.resolveSessionId(null);

        // then
        assertThat(sessionId).isNotBlank();
    }

    @Test
    @DisplayName("[성공] 사용자별 세션 key에서 최근 대화 이력을 조회한다")
    void load_recent_messages() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("ai:chat:session:1:session-1"))
                .willReturn("""
                        [
                          {"role":"USER","content":"분리불안 말티즈 시터 찾아줘"},
                          {"role":"ASSISTANT","content":"시터 #7님을 추천드려요."}
                        ]
                        """);

        // when
        List<AiChatSessionMessage> messages = sessionStore.loadRecentMessages(1L, "session-1");

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).content()).contains("말티즈");
    }

    @Test
    @DisplayName("[성공] 세션 저장 시 최근 N턴만 남기고 TTL을 갱신한다")
    void append_turn_trim_and_refresh_ttl() {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("ai:chat:session:1:session-1"))
                .willReturn("""
                        [
                          {"role":"USER","content":"1번 질문"},
                          {"role":"ASSISTANT","content":"1번 답변"},
                          {"role":"USER","content":"2번 질문"},
                          {"role":"ASSISTANT","content":"2번 답변"}
                        ]
                        """);

        // when
        sessionStore.appendTurn(1L, "session-1", "3번 질문", "3번 답변");

        // then
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        then(valueOperations).should().set(
                eq("ai:chat:session:1:session-1"),
                jsonCaptor.capture(),
                eq(Duration.ofMinutes(30))
        );
        assertThat(jsonCaptor.getValue()).contains("3번 답변");
        assertThat(jsonCaptor.getValue()).doesNotContain("1번 질문");
    }

    @Test
    @DisplayName("[성공] Redis 장애가 나도 챗봇 흐름은 막지 않는다")
    void fail_open_when_redis_failed() {
        // given
        given(stringRedisTemplate.opsForValue()).willThrow(new IllegalStateException("redis down"));

        // when & then
        assertThatCode(() -> sessionStore.loadRecentMessages(1L, "session-1"))
                .doesNotThrowAnyException();
        assertThatCode(() -> sessionStore.appendTurn(1L, "session-1", "질문", "답변"))
                .doesNotThrowAnyException();
    }
}
