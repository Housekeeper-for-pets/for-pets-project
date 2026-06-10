package com.forpets.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Component
public class SseStreamSupport {

    /**
     * 알림처럼 장기 연결을 보관하지 않는 일회성 스트리밍 응답에서 공통으로 사용할 SseEmitter 생성 유틸입니다.
     */
    public SseEmitter createEmitter(long timeoutMs, String streamName) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitter.onCompletion(() -> log.debug("{} SSE 스트림 완료", streamName));
        emitter.onTimeout(() -> {
            log.warn("{} SSE 스트림 타임아웃", streamName);
            emitter.complete();
        });
        emitter.onError(exception -> log.warn("{} SSE 스트림 에러", streamName, exception));
        return emitter;
    }

    public void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
    }

    public void sendError(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", message);
        } catch (IOException exception) {
            log.debug("SSE error event 전송 실패", exception);
        }
    }

    public void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException exception) {
            log.debug("SSE 스트림이 이미 종료되었습니다.", exception);
        }
    }
}
