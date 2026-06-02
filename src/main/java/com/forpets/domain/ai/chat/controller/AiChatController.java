package com.forpets.domain.ai.chat.controller;

import com.forpets.domain.ai.chat.dto.AiChatRequest;
import com.forpets.domain.ai.chat.dto.AiChatResponse;
import com.forpets.domain.ai.chat.service.AiChatService;
import com.forpets.global.common.ApiResponse;
import com.forpets.global.security.annotation.LoginUser;
import com.forpets.global.security.dto.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/api/ai/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @LoginUser CurrentMember currentMember,
            @RequestBody @Valid AiChatRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiChatService.chat(currentMember.id(), request)));
    }
}
