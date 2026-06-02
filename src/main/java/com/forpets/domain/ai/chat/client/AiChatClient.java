package com.forpets.domain.ai.chat.client;

import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;

import java.util.List;

public interface AiChatClient {

    AiSitterSearchCondition extractCondition(String message);

    String generateAnswer(String message, AiSitterSearchCondition condition, List<RecommendedSitterDto> candidates);
}
