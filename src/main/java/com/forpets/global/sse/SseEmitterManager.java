package com.forpets.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private static final String NAME = SseEmitterManager.class.getSimpleName();

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;


    public SseEmitter connect(Long userId) {
        SseEmitter oldEmitter = emitters.get(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("{} => SSE 연결 종료: userId={}",NAME, userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("{} => SSE 타임아웃: userId={}", NAME, userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId);
            log.warn("{} => SSE 에러: userId={}", NAME, userId, e);
        });

        emitters.put(userId, emitter);
        log.info("{} => SSE 연결: userId={}", NAME, userId);

        // 최초 연결 시 더미 이벤트 전송 (503 방지)
        sendToUser(userId, "firstConnect", "연결 성공");
        return emitter;
    }


    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("SSE 미연결 유저: userId={}", userId);
            return;  // 연결 안 돼있으면 그냥 패스 (DB에는 이미 저장됨)
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("SSE 전송 실패, 연결 정리: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        }
    }


    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}