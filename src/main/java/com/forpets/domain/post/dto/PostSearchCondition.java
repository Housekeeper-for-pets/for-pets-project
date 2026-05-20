package com.forpets.domain.post.dto;

import com.forpets.domain.member.entity.Region;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.global.common.CareType;

/**
 * 공고 목록 검색 조건 DTO입니다.
 */
public record PostSearchCondition(
        Region region,
        CareType careType,
        PostStatus status,
        String keyword
) {
}
