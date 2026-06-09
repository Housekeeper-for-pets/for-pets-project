package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 캐시 키 생성기 단위 테스트.
 * gender가 검색 조건에 포함되므로, gender가 다르면 캐시 키도 달라져야 한다(키 충돌 방지).
 */
class SitterCacheKeyGeneratorTest {

    private SitterSearchCondition condition(MemberGender gender) {
        return new SitterSearchCondition(Region.GANGNAM, null, null, null, null, gender);
    }

    @Test
    @DisplayName("[성공] 동일한 검색 조건은 동일한 캐시 키를 생성한다")
    void same_condition_produces_same_key() {
        String key1 = SitterCacheKeyGenerator.generate(condition(MemberGender.MALE), 0, 10, "createdAt", "desc");
        String key2 = SitterCacheKeyGenerator.generate(condition(MemberGender.MALE), 0, 10, "createdAt", "desc");

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("[성공] gender만 다른 검색 조건은 서로 다른 캐시 키를 생성한다")
    void different_gender_produces_different_key() {
        String male = SitterCacheKeyGenerator.generate(condition(MemberGender.MALE), 0, 10, "createdAt", "desc");
        String female = SitterCacheKeyGenerator.generate(condition(MemberGender.FEMALE), 0, 10, "createdAt", "desc");
        String none = SitterCacheKeyGenerator.generate(condition(null), 0, 10, "createdAt", "desc");

        assertThat(male).isNotEqualTo(female);
        assertThat(male).isNotEqualTo(none);
        assertThat(female).isNotEqualTo(none);
    }
}
