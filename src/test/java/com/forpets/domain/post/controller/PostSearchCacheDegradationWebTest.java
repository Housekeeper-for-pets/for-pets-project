package com.forpets.domain.post.controller;

import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.post.dto.PostSearchCondition;
import com.forpets.domain.post.repository.PostPetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.post.service.PostCacheKeyGenerator;
import com.forpets.domain.post.service.PostCacheService;
import com.forpets.domain.post.service.PostService;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import com.forpets.global.embed.TimeSlotValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {
        PostCacheService.class,
        PostCacheKeyGenerator.class,
        GracefulDegradationCacheTestConfiguration.class,
        PostSearchCacheDegradationWebTest.MockConfig.class
})
class PostSearchCacheDegradationWebTest {

    @Autowired
    private PostCacheService postCacheService;

    @Autowired
    private PostRepository postRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostService postService = new PostService(
                postRepository,
                mock(PostPetRepository.class),
                mock(PostTimeSlotRepository.class),
                mock(PetService.class),
                mock(TimeSlotValidator.class),
                mock(MemberService.class),
                mock(ProposalRepository.class),
                postCacheService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postService)).build();
    }

    @Test
    @DisplayName("GET /api/posts — 캐시 장애 시 200 응답 및 DB 폴백")
    void search_returns_200_when_cache_fails() throws Exception {
        PostPageResponse response = PostPageResponse.of(List.of(), 0, 0, 0, 10);
        given(postRepository.searchPosts(eq(new PostSearchCondition(null, null, null, null)), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        PostRepository postRepository() {
            return mock(PostRepository.class);
        }
    }
}
