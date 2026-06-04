package com.forpets.domain.ai.chat.dto;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;

public record AiSitterSearchCondition(
        Region region,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer minPrice,
        Integer maxPrice,
        String concern
) {
    public static AiSitterSearchCondition empty() {
        return new AiSitterSearchCondition(null, null, null, null, null, null);
    }
}
