package com.forpets.domain.post.service;

import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
        PostCacheService.class,
        PostCacheKeyGenerator.class,
        GracefulDegradationCacheTestConfiguration.class,
        PostCacheServiceGracefulDegradationTest.MockConfig.class
})
class PostCacheServiceGracefulDegradationTest {

    @Autowired
    private PostCacheService postCacheService;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("캐시 GET 실패 시 Repository 조회 결과를 반환한다 (DB 폴백)")
    void searchPostings_falls_back_to_repository_when_cache_fails() {
        PostSearchCondition condition = new PostSearchCondition(null, null, null, null);
        PostPageResponse expected = PostPageResponse.of(List.of(), 0, 0, 0, 10);

        given(postRepository.searchPosts(eq(condition), any(Pageable.class)))
                .willReturn(expected);

        PostPageResponse result = postCacheService.searchPostings(condition, 0, 10, "createdAt");

        assertThat(result).isEqualTo(expected);
        then(postRepository).should().searchPosts(eq(condition), any(Pageable.class));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        PostRepository postRepository() {
            return mock(PostRepository.class);
        }
    }
}
