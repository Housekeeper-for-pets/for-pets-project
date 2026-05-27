package com.forpets.domain.admin.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.admin.AdminSitterResponseDto;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterCacheService;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {
        SitterAdminService.class,
        SitterCacheService.class,
        GracefulDegradationCacheTestConfiguration.class,
        SitterAdminServiceCacheEvictGracefulDegradationTest.MockConfig.class
})
class SitterAdminServiceCacheEvictGracefulDegradationTest {

    @Autowired
    private SitterAdminService sitterAdminService;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    @Autowired
    private MemberService memberService;

    private final Long adminId = 1L;
    private final Long sitterId = 100L;
    private final Long memberId = 10L;

    private SitterProfile sitter;
    private Member member;

    @BeforeEach
    void setUp() {
        sitter = SitterProfile.builder()
                .memberId(memberId)
                .introduction("대기 시터")
                .experienceYears(2)
                .possiblePetType(PossiblePetType.DOG)
                .possiblePetSize(PossiblePetSize.SMALL)
                .pricePerHour(12000)
                .build();
        ReflectionTestUtils.setField(sitter, "id", sitterId);

        member = Member.builder()
                .email("pending@test.com")
                .password("password")
                .nickname("pending")
                .phone("010-2222-2222")
                .gender(MemberGender.FEMALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
    }

    @Test
    @DisplayName("캐시 EVICT 실패해도 시터 승인은 정상 완료된다")
    void approve_completes_when_cache_evict_fails() {
        given(sitterProfileRepository.findById(sitterId)).willReturn(Optional.of(sitter));
        given(memberService.findById(memberId)).willReturn(member);

        assertThatCode(() -> {
            AdminSitterResponseDto result = sitterAdminService.approve(adminId, sitterId);
            assertThat(result.approvalStatus()).isEqualTo(SitterApprovalStatus.APPROVED);
        }).doesNotThrowAnyException();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        SitterProfileRepository sitterProfileRepository() {
            return mock(SitterProfileRepository.class);
        }

        @Bean
        MemberService memberService() {
            return mock(MemberService.class);
        }

        @Bean
        com.forpets.domain.sitter.repository.SitterScheduleRepository sitterScheduleRepository() {
            return mock(com.forpets.domain.sitter.repository.SitterScheduleRepository.class);
        }
    }
}
