package com.forpets.domain.sitter.controller;

import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.domain.sitter.service.SitterCacheKeyGenerator;
import com.forpets.domain.sitter.service.SitterCacheService;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import com.forpets.global.common.AssociationChecker;
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
        SitterCacheService.class,
        SitterCacheKeyGenerator.class,
        GracefulDegradationCacheTestConfiguration.class,
        SitterSearchCacheDegradationWebTest.MockConfig.class
})
class SitterSearchCacheDegradationWebTest {

    @Autowired
    private SitterCacheService sitterCacheService;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SitterService sitterService = new SitterService(
                mock(MemberService.class),
                sitterProfileRepository,
                mock(SitterScheduleRepository.class),
                mock(AssociationChecker.class),
                sitterCacheService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new SitterController(sitterService)).build();
    }

    @Test
    @DisplayName("GET /api/sitters — 캐시 장애 시 200 응답 및 DB 폴백")
    void search_returns_200_when_cache_fails() throws Exception {
        SitterPageResponse response = SitterPageResponse.of(List.of(), 0, 0, 0, 10);
        given(sitterProfileRepository.searchSitters(eq(new SitterSearchCondition(null, null, null, null, null, null)), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(get("/api/sitters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
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
