package com.forpets.domain.sitter.service;

import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {
        SitterCacheService.class,
        SitterCacheKeyGenerator.class,
        GracefulDegradationCacheTestConfiguration.class,
        SitterCacheServiceGracefulDegradationTest.MockConfig.class
})
class SitterCacheServiceGracefulDegradationTest {

    @Autowired
    private SitterCacheService sitterCacheService;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    @Test
    @DisplayName("캐시 GET 실패 시 Repository 조회 결과를 반환한다 (DB 폴백)")
    void searchSitters_falls_back_to_repository_when_cache_fails() {
        SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null, null);
        SitterPageResponse expected = SitterPageResponse.of(List.of(), 0, 0, 0, 10);

        given(sitterProfileRepository.searchSitters(eq(condition), any(Pageable.class)))
                .willReturn(expected);

        SitterPageResponse result = sitterCacheService.searchSitters(condition, 0, 10, "createdAt", "desc");

        assertThat(result).isEqualTo(expected);
        then(sitterProfileRepository).should().searchSitters(eq(condition), any(Pageable.class));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        SitterProfileRepository sitterProfileRepository() {
            return mock(SitterProfileRepository.class);
        }

        @Bean
        SitterScheduleRepository sitterScheduleRepository() {
            return mock(SitterScheduleRepository.class);
        }

        @Bean
        MemberService memberService() {
            return mock(MemberService.class);
        }
    }
}
