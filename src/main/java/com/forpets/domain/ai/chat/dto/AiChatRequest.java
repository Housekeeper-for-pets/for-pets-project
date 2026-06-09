package com.forpets.domain.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "메시지는 필수입니다")
        @Size(max = 500, message = "메시지는 500자 이하로 입력해주세요")
        String message
) {
}
