package com.forpets.domain.ai.chat.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.service.AiChatService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import com.forpets.global.sse.SseStreamSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;
    private final SseStreamSupport sseStreamSupport;

    @Value("${forpets.ai.chat.stream-timeout-ms:60000}")
    private long streamTimeoutMs;

    @PostMapping("/api/ai/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid AiChatRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.chat(currentMember.id(), request)));
    }

    @PostMapping(value = "/api/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid AiChatRequest request
    ) {
        SseEmitter emitter = sseStreamSupport.createEmitter(streamTimeoutMs, "AI_CHAT");

        CompletableFuture.runAsync(() -> {
            try {
                AiChatResponse response = aiChatService.chat(currentMember.id(), request);
                sseStreamSupport.send(emitter, "session", response.sessionId());
                for (String chunk : splitAnswer(response.answer())) {
                    sseStreamSupport.send(emitter, "message", chunk);
                }
                sseStreamSupport.send(emitter, "sources", toJson(response.sources()));
                sseStreamSupport.send(emitter, "done", toJson(response));
                emitter.complete();
            } catch (Exception exception) {
                log.warn("AI 챗봇 SSE 응답 실패", exception);
                sseStreamSupport.sendError(emitter, "AI 추천 응답을 스트리밍하는 중 문제가 발생했습니다.");
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    private List<String> splitAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of("");
        }
        return answer.lines()
                .flatMap(line -> splitLine(line).stream())
                .toList();
    }

    private List<String> splitLine(String line) {
        if (line.length() <= 80) {
            return List.of(line + "\n");
        }

        return java.util.stream.IntStream.iterate(0, index -> index < line.length(), index -> index + 80)
                .mapToObj(index -> line.substring(index, Math.min(index + 80, line.length())))
                .map(chunk -> chunk + "\n")
                .toList();
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }
}
