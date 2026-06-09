package com.forpets.domain.ai.chat.dto;

public record AiChatSessionMessage(
        String role,
        String content
) {

    public static AiChatSessionMessage user(String content) {
        return new AiChatSessionMessage("USER", content);
    }

    public static AiChatSessionMessage assistant(String content) {
        return new AiChatSessionMessage("ASSISTANT", content);
    }
}
