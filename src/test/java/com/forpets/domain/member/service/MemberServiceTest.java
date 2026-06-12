package com.forpets.domain.member.service;

import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.coupon.repository.UserCouponRepository;
import com.forpets.domain.member.dto.UpdateMemberRequest;
import com.forpets.domain.member.dto.UpdateMemberResponse;
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.repository.MemberRepository;
import com.forpets.domain.pet.repository.PetRepository;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterCacheEvictor;
import com.forpets.global.security.jwt.BearerTokenResolver;
import com.forpets.global.security.jwt.JwtTokenProvider;
import com.forpets.global.security.jwt.TokenRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final Long MEMBER_ID = 3L;
    private static final Long SITTER_ID = 100L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private CareRequestRepository careRequestRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private TokenRedisService tokenRedisService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BearerTokenResolver bearerTokenResolver;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private SitterCacheEvictor sitterCacheEvictor;

    private MemberService memberService;
    private Member member;
    private SitterProfile sitterProfile;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                memberRepository,
                passwordEncoder,
                reservationRepository,
                postRepository,
                proposalRepository,
                sitterProfileRepository,
                careRequestRepository,
                petRepository,
                tokenRedisService,
                jwtTokenProvider,
                bearerTokenResolver,
                userCouponRepository,
                sitterCacheEvictor
        );

        member = Member.builder()
                .email("sitter@test.com")
                .password("password")
                .nickname("oldNickname")
                .phone("010-1111-1111")
                .gender(MemberGender.MALE)
                .region(Region.GWANGJIN)
                .build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);

        sitterProfile = SitterProfile.builder()
                .memberId(MEMBER_ID)
                .introduction("intro")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.CAT)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(30000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", SITTER_ID);
    }

    @Nested
    @DisplayName("updateMyInfo")
    class UpdateMyInfoTest {

        @Test
        @DisplayName("evicts sitter detail and list caches when member-owned sitter response fields change")
        void evicts_sitter_caches_when_region_gender_or_nickname_changes() {
            UpdateMemberRequest request = new UpdateMemberRequest(
                    "newNickname",
                    "010-1111-1111",
                    MemberGender.FEMALE,
                    Region.NOWON
            );
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(memberRepository.countByNicknameIncludingDeleted("newNickname")).willReturn(0);
            given(sitterProfileRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(sitterProfile));

            UpdateMemberResponse response = memberService.updateMyInfo(MEMBER_ID, request);

            assertThat(response.nickname()).isEqualTo("newNickname");
            assertThat(response.gender()).isEqualTo(MemberGender.FEMALE);
            assertThat(response.region()).isEqualTo(Region.NOWON);
            then(sitterCacheEvictor).should().evictSitterDetail(SITTER_ID);
            then(sitterCacheEvictor).should().evictSitterList();
        }

        @Test
        @DisplayName("does not evict sitter caches when only phone changes")
        void does_not_evict_sitter_caches_when_only_phone_changes() {
            UpdateMemberRequest request = new UpdateMemberRequest(
                    "oldNickname",
                    "010-2222-2222",
                    MemberGender.MALE,
                    Region.GWANGJIN
            );
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            UpdateMemberResponse response = memberService.updateMyInfo(MEMBER_ID, request);

            assertThat(response.phone()).isEqualTo("010-2222-2222");
            then(sitterProfileRepository).should(never()).findByMemberId(MEMBER_ID);
            then(sitterCacheEvictor).shouldHaveNoInteractions();
        }
    }
}
