package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;

/**
 * 시터 목록 검색 조건 DTO입니다.
 * Controller → Service → Repository 로 검색 파라미터를 전달합니다.
 */
public record SitterSearchCondition(
        Region region,
        PossiblePetType possiblePetType,
        PossiblePetSize possiblePetSize,
        Integer minPrice,
        Integer maxPrice
) {
}
