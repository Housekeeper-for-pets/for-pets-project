package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.UpdateSitterRequest;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.cache.support.GracefulDegradationCacheTestConfiguration;
import com.forpets.global.common.AssociationChecker;
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
        SitterService.class,
        SitterCacheService.class,
        GracefulDegradationCacheTestConfiguration.class,
        SitterServiceCacheEvictGracefulDegradationTest.MockConfig.class
})
class SitterServiceCacheEvictGracefulDegradationTest {

    @Autowired
    private SitterService sitterService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SitterProfileRepository sitterProfileRepository;

    private final Long memberId = 1L;
    private final Long adminId = 99L;
    private final Long sitterProfileId = 100L;

    private Member member;
    private SitterProfile sitterProfile;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("sitter@test.com")
                .password("password")
                .nickname("sitter")
                .phone("010-1111-1111")
                .gender(MemberGender.MALE)
                .region(Region.SEOCHO)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        sitterProfile = SitterProfile.builder()
                .memberId(memberId)
                .introduction("소개")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.DOG)
                .possiblePetSize(PossiblePetSize.SMALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterProfileId);
        sitterProfile.approve(adminId);
    }

    @Test
    @DisplayName("캐시 EVICT 실패해도 시터 프로필 수정은 정상 완료된다")
    void update_completes_when_cache_evict_fails() {
        UpdateSitterRequest request = new UpdateSitterRequest(
                "수정된 소개", 5, PossiblePetType.CAT, PossiblePetSize.MEDIUM, 20000
        );

        given(memberService.findById(memberId)).willReturn(member);
        given(sitterProfileRepository.findByMemberId(memberId)).willReturn(Optional.of(sitterProfile));

        assertThatCode(() -> {
            SitterResponseDto result = sitterService.update(memberId, request);
            assertThat(result.introduction()).isEqualTo("수정된 소개");
            assertThat(result.approvalStatus()).isEqualTo(SitterApprovalStatus.APPROVED);
        }).doesNotThrowAnyException();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        MemberService memberService() {
            return mock(MemberService.class);
        }

        @Bean
        SitterProfileRepository sitterProfileRepository() {
            return mock(SitterProfileRepository.class);
        }

        @Bean
        SitterScheduleRepository sitterScheduleRepository() {
            return mock(SitterScheduleRepository.class);
        }

        @Bean
        AssociationChecker associationChecker() {
            return mock(AssociationChecker.class);
        }
    }
}
