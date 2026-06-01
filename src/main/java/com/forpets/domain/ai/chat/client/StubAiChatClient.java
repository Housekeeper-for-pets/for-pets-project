package com.forpets.domain.ai.chat.client;

import com.forpets.domain.ai.chat.dto.AiSitterSearchCondition;
import com.forpets.domain.ai.chat.dto.RecommendedSitterDto;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!ai & !openai")
public class StubAiChatClient implements AiChatClient {

    @Override
    public AiSitterSearchCondition extractCondition(String message) {
        String lowerMessage = message == null ? "" : message.toLowerCase();

        Region region = lowerMessage.contains("마포") ? Region.MAPO : null;
        PossiblePetType petType = lowerMessage.contains("고양이") || lowerMessage.contains("cat")
                ? PossiblePetType.CAT
                : lowerMessage.contains("강아지") || lowerMessage.contains("dog") || lowerMessage.contains("말티즈")
                ? PossiblePetType.DOG
                : null;
        PossiblePetSize petSize = lowerMessage.contains("소형") || lowerMessage.contains("말티즈")
                ? PossiblePetSize.SMALL
                : null;
        String concern = lowerMessage.contains("분리불안") ? "분리불안" : null;

        return new AiSitterSearchCondition(region, petType, petSize, null, null, concern);
    }

    @Override
    public String generateAnswer(String message, AiSitterSearchCondition condition, List<RecommendedSitterDto> candidates) {
        if (candidates.isEmpty()) {
            return "조건에 맞는 시터를 찾지 못했어요. 지역이나 반려동물 조건을 조금 넓혀서 다시 검색해보세요.";
        }

        RecommendedSitterDto first = candidates.get(0);
        String summary = first.reviewSummary() == null
                ? "아직 리뷰 요약은 없지만, 시터 프로필 조건은 요청과 잘 맞습니다."
                : first.reviewSummary();

        return "조건에 맞는 시터 " + candidates.size() + "명을 찾았어요. "
                + "우선 " + first.region().getDescription() + " 지역의 시터를 추천드릴게요. "
                + summary;
    }
}
