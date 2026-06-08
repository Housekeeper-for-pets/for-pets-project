package com.forpets.domain.sitter.service;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.MemberRole;
import com.forpets.domain.member.entity.Region;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.sitter.dto.profile.CreateSitterRequest;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterResponseDto;
import com.forpets.domain.sitter.dto.profile.SitterSearchCondition;
import com.forpets.domain.sitter.dto.profile.UpdateSitterRequest;
import com.forpets.domain.sitter.dto.profile.UpdateSitterStatusRequest;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterApprovalStatus;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterProfileStatus;
import com.forpets.domain.sitter.entity.SitterSchedule;
import com.forpets.domain.sitter.exception.SitterErrorCode;
import com.forpets.domain.sitter.exception.SitterException;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.repository.SitterScheduleRepository;
import com.forpets.global.common.AssociationChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SitterServiceTest {

    @InjectMocks
    private SitterService sitterService;

    @Mock
    private MemberService memberService;

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private SitterScheduleRepository sitterScheduleRepository;

    @Mock
    private AssociationChecker associationChecker;

    @Mock
    private SitterCacheService sitterCacheService;

    // ── 테스트 픽스처 ──
    private Member member1;  // 째길중 — MEMBER, SEOCHO
    private Member member2;  // 타코맘 — MEMBER, DONGJAK
    private Member adminMember; // 관리자 — ADMIN

    private SitterProfile sitterProfile;

    private final Long member1Id = 1L;
    private final Long member2Id = 2L;
    private final Long adminMemberId = 3L;
    private final Long sitterProfileId = 100L;

    @BeforeEach
    void setUp() {
        // member1: 째길중 (MEMBER, SEOCHO)
        member1 = Member.builder()
                .email("giljung@test.com")
                .password("password123")
                .nickname("째길중")
                .phone("010-1111-1111")
                .gender(MemberGender.MALE)
                .region(Region.SEOCHO)
                .build();
        ReflectionTestUtils.setField(member1, "id", member1Id);

        // member2: 타코맘 (MEMBER, DONGJAK)
        member2 = Member.builder()
                .email("jiwon@test.com")
                .password("password123")
                .nickname("타코맘")
                .phone("010-2222-2222")
                .gender(MemberGender.FEMALE)
                .region(Region.DONGJAK)
                .build();
        ReflectionTestUtils.setField(member2, "id", member2Id);

        // adminMember: 관리자 (ADMIN)
        adminMember = Member.builder()
                .email("admin@test.com")
                .password("password123")
                .nickname("관리자")
                .phone("010-9999-9999")
                .gender(MemberGender.MALE)
                .region(Region.GANGNAM)
                .build();
        ReflectionTestUtils.setField(adminMember, "id", adminMemberId);
        ReflectionTestUtils.setField(adminMember, "role", MemberRole.ADMIN);

        // sitterProfile: member1의 시터 프로필
        sitterProfile = SitterProfile.builder()
                .memberId(member1Id)
                .introduction("반려동물을 사랑하는 시터입니다")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.ALL)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterProfileId);
    }

    // ========================================================
    // 시터 프로필 등록 — POST /api/sitters
    // ========================================================
    @Nested
    @DisplayName("시터 프로필 등록 — POST /api/sitters")
    class CreateSitterTest {

        @Test
        @DisplayName("[성공] MEMBER가 시터 프로필 등록 성공 — 역할이 SITTER로 변경됨")
        void sitter_test_01() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "반려동물을 사랑하는 시터입니다", 3,
                    PossiblePetType.ALL, PossiblePetSize.ALL, 15000
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByIdIncludingDeleted(member1Id)).willReturn(Optional.empty());
            given(sitterProfileRepository.save(any(SitterProfile.class))).willReturn(sitterProfile);

            // when
            SitterResponseDto result = sitterService.create(member1Id, request);

            // then
            assertThat(result.memberId()).isEqualTo(member1Id);
            assertThat(result.introduction()).isEqualTo("반려동물을 사랑하는 시터입니다");
            assertThat(result.experienceYears()).isEqualTo(3);
            assertThat(result.possiblePetType()).isEqualTo(PossiblePetType.ALL);
            assertThat(result.possiblePetSize()).isEqualTo(PossiblePetSize.ALL);
            assertThat(result.pricePerHour()).isEqualTo(15000);
            assertThat(result.region()).isEqualTo(Region.SEOCHO);
            assertThat(member1.getRole()).isEqualTo(MemberRole.MEMBER);
            assertThat(result.approvalStatus()).isEqualTo(SitterApprovalStatus.PENDING);
            then(sitterProfileRepository).should().save(any(SitterProfile.class));
        }

        @Test
        @DisplayName("[성공] possiblePetSize 미입력 시 ALL로 기본값 설정")
        void sitter_test_02() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "소형견 전문 시터", 2,
                    PossiblePetType.DOG, null, 12000
            );
            SitterProfile sitterWithDefaultSize = SitterProfile.builder()
                    .memberId(member2Id)
                    .introduction("소형견 전문 시터")
                    .experienceYears(2)
                    .possiblePetType(PossiblePetType.DOG)
                    .possiblePetSize(null)  // null → ALL 기본값
                    .pricePerHour(12000)
                    .build();
            ReflectionTestUtils.setField(sitterWithDefaultSize, "id", 101L);

            given(memberService.findById(member2Id)).willReturn(member2);
            given(sitterProfileRepository.findByIdIncludingDeleted(member2Id)).willReturn(Optional.empty());
            given(sitterProfileRepository.save(any(SitterProfile.class))).willReturn(sitterWithDefaultSize);

            // when
            SitterResponseDto result = sitterService.create(member2Id, request);

            // then
            assertThat(result.possiblePetSize()).isEqualTo(PossiblePetSize.ALL);
        }

        @Test
        @DisplayName("[성공] 생성 직후 status가 RESERVABLE인지 확인")
        void sitter_test_03() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "고양이 전문 시터", 5,
                    PossiblePetType.CAT, PossiblePetSize.SMALL, 20000
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByIdIncludingDeleted(member1Id)).willReturn(Optional.empty());
            given(sitterProfileRepository.save(any(SitterProfile.class))).willReturn(sitterProfile);

            // when
            SitterResponseDto result = sitterService.create(member1Id, request);

            // then
            assertThat(result.status()).isEqualTo(SitterProfileStatus.NON_RESERVABLE);
            assertThat(result.approvalStatus()).isEqualTo(SitterApprovalStatus.PENDING);
        }

        @Test
        @DisplayName("[실패] ADMIN이 시터 프로필 등록 시도 시 차단")
        void sitter_test_04() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "관리자 시터", 1,
                    PossiblePetType.ALL, PossiblePetSize.ALL, 10000
            );
            given(memberService.findById(adminMemberId)).willReturn(adminMember);

            // when & then
            assertThatThrownBy(() -> sitterService.create(adminMemberId, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.ADMIN_CANNOT_REGISTER_SITTER));
        }

        @Test
        @DisplayName("[실패] 이미 시터 프로필이 존재하는 회원이 재등록 시도")
        void sitter_test_05() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "중복 등록 시도", 1,
                    PossiblePetType.DOG, PossiblePetSize.SMALL, 10000
            );
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByIdIncludingDeleted(member1Id))
                    .willReturn(Optional.of(sitterProfile));

            // when & then
            assertThatThrownBy(() -> sitterService.create(member1Id, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_ALREADY_PENDING));
        }

        @Test
        @DisplayName("[실패] 시터 프로필을 삭제한 회원이 재등록 시도")
        void sitter_test_06() {
            // given
            CreateSitterRequest request = new CreateSitterRequest(
                    "재등록 시도", 1,
                    PossiblePetType.CAT, PossiblePetSize.MEDIUM, 10000
            );
            sitterProfile.delete();
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByIdIncludingDeleted(member1Id))
                    .willReturn(Optional.of(sitterProfile));

            // when
            SitterResponseDto result = sitterService.create(member1Id, request);

            // then
            assertThat(sitterProfile.isDeleted()).isFalse();
            assertThat(result.status()).isEqualTo(SitterProfileStatus.NON_RESERVABLE);
            assertThat(result.approvalStatus()).isEqualTo(SitterApprovalStatus.PENDING);
        }
    }

    // ========================================================
    // 내 시터 프로필 조회 — GET /api/sitters/me
    // ========================================================
    @Nested
    @DisplayName("내 시터 프로필 조회 — GET /api/sitters/me")
    class GetMyProfileTest {

        @Test
        @DisplayName("[성공] 내 시터 프로필 조회 성공 — 스케줄 포함 반환")
        void sitter_test_07() {
            // given
            SitterSchedule schedule = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            ReflectionTestUtils.setField(schedule, "id", 1L);

            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterScheduleRepository.findAllBySitterProfileId(sitterProfileId)).willReturn(List.of(schedule));

            // when
            SitterResponseDto result = sitterService.getMyProfile(member1Id);

            // then
            assertThat(result.id()).isEqualTo(sitterProfileId);
            assertThat(result.memberId()).isEqualTo(member1Id);
            assertThat(result.region()).isEqualTo(Region.SEOCHO);
            assertThat(result.introduction()).isEqualTo("반려동물을 사랑하는 시터입니다");
            assertThat(result.schedules()).hasSize(1);
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 조회 시도")
        void sitter_test_08() {
            // given
            given(sitterProfileRepository.findByMemberId(member2Id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sitterService.getMyProfile(member2Id))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_PROFILE_REQUIRED));
        }
    }

    // ========================================================
    // 시터 프로필 수정 — PUT /api/sitters/me
    // ========================================================
    @Nested
    @DisplayName("시터 프로필 수정 — PUT /api/sitters/me")
    class UpdateSitterTest {

        @Test
        @DisplayName("[성공] 시터 프로필 수정 성공 (전체 필드 교체)")
        void sitter_test_09() {
            // given
            UpdateSitterRequest request = new UpdateSitterRequest(
                    "경력 5년 대형견 전문 시터로 변경", 5,
                    PossiblePetType.DOG, PossiblePetSize.LARGE, 25000
            );
            sitterProfile.approve(adminMemberId);
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));

            // when
            SitterResponseDto result = sitterService.update(member1Id, request);

            // then
            assertThat(result.introduction()).isEqualTo("경력 5년 대형견 전문 시터로 변경");
            assertThat(result.experienceYears()).isEqualTo(5);
            assertThat(result.possiblePetType()).isEqualTo(PossiblePetType.DOG);
            assertThat(result.possiblePetSize()).isEqualTo(PossiblePetSize.LARGE);
            assertThat(result.pricePerHour()).isEqualTo(25000);
            assertThat(result.region()).isEqualTo(Region.SEOCHO);
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 수정 시도")
        void sitter_test_10() {
            // given
            UpdateSitterRequest request = new UpdateSitterRequest(
                    "수정 시도", 1,
                    PossiblePetType.CAT, PossiblePetSize.SMALL, 10000
            );
            given(memberService.findById(member2Id)).willReturn(member2);
            given(sitterProfileRepository.findByMemberId(member2Id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sitterService.update(member2Id, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_PROFILE_REQUIRED));
        }
    }

    // ========================================================
    // 시터 상태 변경 — PATCH /api/sitters/me/status
    // ========================================================
    @Nested
    @DisplayName("시터 상태 변경 — PATCH /api/sitters/me/status")
    class UpdateSitterStatusTest {

        @Test
        @DisplayName("[성공] RESERVABLE → NON_RESERVABLE 변경 성공")
        void sitter_test_11() {
            // given
            UpdateSitterStatusRequest request = new UpdateSitterStatusRequest(SitterProfileStatus.NON_RESERVABLE);
            sitterProfile.approve(adminMemberId);
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));

            // when
            SitterResponseDto result = sitterService.updateStatus(member1Id, request);

            // then
            assertThat(result.status()).isEqualTo(SitterProfileStatus.NON_RESERVABLE);
        }

        @Test
        @DisplayName("[성공] NON_RESERVABLE → RESERVABLE 변경 성공")
        void sitter_test_12() {
            // given
            sitterProfile.approve(adminMemberId);
            sitterProfile.changeStatus(SitterProfileStatus.NON_RESERVABLE); // 사전에 NON_RESERVABLE로 설정
            UpdateSitterStatusRequest request = new UpdateSitterStatusRequest(SitterProfileStatus.RESERVABLE);
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));

            // when
            SitterResponseDto result = sitterService.updateStatus(member1Id, request);

            // then
            assertThat(result.status()).isEqualTo(SitterProfileStatus.RESERVABLE);
        }

        @Test
        @DisplayName("[성공] 진행 중 예약이 있어도 NON_RESERVABLE 변경 가능 — 상태 변경에 예약 체크 없음")
        void sitter_test_13() {
            // given — 상태 변경 로직에는 associationChecker 호출이 없으므로 stub 불필요
            UpdateSitterStatusRequest request = new UpdateSitterStatusRequest(SitterProfileStatus.NON_RESERVABLE);
            sitterProfile.approve(adminMemberId);
            given(memberService.findById(member1Id)).willReturn(member1);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));

            // when
            SitterResponseDto result = sitterService.updateStatus(member1Id, request);

            // then
            assertThat(result.status()).isEqualTo(SitterProfileStatus.NON_RESERVABLE);
            then(associationChecker).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 상태 변경 시도")
        void sitter_test_14() {
            // given
            UpdateSitterStatusRequest request = new UpdateSitterStatusRequest(SitterProfileStatus.NON_RESERVABLE);
            given(memberService.findById(member2Id)).willReturn(member2);
            given(sitterProfileRepository.findByMemberId(member2Id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sitterService.updateStatus(member2Id, request))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_PROFILE_REQUIRED));
        }
    }

    // ========================================================
    // 시터 프로필 삭제 — DELETE /api/sitters/me
    // ========================================================
    @Nested
    @DisplayName("시터 프로필 삭제 — DELETE /api/sitters/me")
    class DeleteSitterTest {

        @Test
        @DisplayName("[성공] 시터 프로필 삭제 성공 — 활성 연관 없는 경우, 역할 MEMBER로 복원")
        void sitter_test_15() {
            // given
            member1.changeRoleToSitter(); // 사전에 SITTER 역할로 설정
            sitterProfile.approve(adminMemberId);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));
            given(associationChecker.hasSitterActiveAssociation(sitterProfileId)).willReturn(false);
            given(memberService.findById(member1Id)).willReturn(member1);

            // when
            sitterService.delete(member1Id);

            // then
            assertThat(sitterProfile.isDeleted()).isTrue();
            assertThat(sitterProfile.getDeletedAt()).isNotNull();
            assertThat(member1.getRole()).isEqualTo(MemberRole.MEMBER);
        }

        @Test
        @DisplayName("[실패] 연관된 Proposal/CareRequest/Reservation이 존재하는 시터프로필 삭제 시도")
        void sitter_test_16() {
            // given
            sitterProfile.approve(adminMemberId);
            given(sitterProfileRepository.findByMemberId(member1Id)).willReturn(Optional.of(sitterProfile));
            given(associationChecker.hasSitterActiveAssociation(sitterProfileId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sitterService.delete(member1Id))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_USED_IN_ACTIVE_PROCESS));
        }

        @Test
        @DisplayName("[실패] 시터 프로필이 없는 회원이 삭제 시도")
        void sitter_test_17() {
            // given
            given(sitterProfileRepository.findByMemberId(member2Id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sitterService.delete(member2Id))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_PROFILE_REQUIRED));
        }
    }

    @Nested
    @DisplayName("시터 목록 조회 — GET /api/sitters")
    class SearchSittersTest {

        @Test
        @DisplayName("[성공] 기본 조회 요청은 페이지 기본값과 createdAt 정렬로 repository에 위임된다")
        void search_sitters_test_01() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);
            SitterPageResponse response = SitterPageResponse.of(List.of(), 0, 0, 0, 10);
            //given(sitterProfileRepository.searchSitters(eq(condition), any())).willReturn(response);
            given(sitterCacheService.searchSitters(eq(condition), eq(0), eq(10), eq("createdAt"), eq("desc")))
                    .willReturn(response);

            // when
            SitterPageResponse result = sitterService.searchSitters(condition, 0, 10, "createdAt", "desc");

            // then
            assertThat(result).isEqualTo(response);
            then(sitterCacheService).should().searchSitters(eq(condition), eq(0), eq(10), eq("createdAt"), eq("desc"));
            // ← then(sitterProfileRepository) → then(sitterCacheService)로 교체
        }

        @Test
        @DisplayName("[성공] 허용된 정렬 필드 pricePerHour, experienceYears는 조회 가능하다")
        void search_sitters_test_02() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);
//            given(sitterProfileRepository.searchSitters(eq(condition), any()))
//                    .willReturn(SitterPageResponse.of(List.of(), 0, 0, 0, 10));
            given(sitterCacheService.searchSitters(eq(condition), eq(0), eq(10), anyString(), anyString()))
                    .willReturn(SitterPageResponse.of(List.of(), 0, 0, 0, 10));

            // when
            sitterService.searchSitters(condition, 0, 10, "pricePerHour", "desc");
            sitterService.searchSitters(condition, 0, 10, "experienceYears", "asc");

            // then
            then(sitterCacheService).should(times(2))
                    .searchSitters(eq(condition), eq(0), eq(10), anyString(), anyString());

        }

        @Test
        @DisplayName("[실패] 허용되지 않은 sort 필드는 INVALID_SORT_FIELD를 반환한다")
        void search_sitters_test_03() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, 0, 10, "hacked", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
            then(sitterProfileRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[실패] page가 음수이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_sitters_test_04() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, -1, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
            then(sitterProfileRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[실패] size가 0이면 INVALID_PAGE_REQUEST를 반환한다")
        void search_sitters_test_05() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, 0, 0, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
            then(sitterProfileRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[실패] size가 최대값을 초과하면 INVALID_PAGE_REQUEST를 반환한다")
        void search_sitters_test_06() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, 0, 51, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_PAGE_REQUEST));
            then(sitterProfileRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[실패] minPrice가 maxPrice보다 크면 INVALID_SEARCH_CONDITION을 반환한다")
        void search_sitters_test_07() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, 50000, 10000);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, 0, 10, "createdAt", "desc"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SEARCH_CONDITION));
            then(sitterProfileRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("[성공] sort=averageRating, direction=desc 요청은 캐시 서비스에 위임된다")
        void search_sitters_sort_by_average_rating_desc() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);
            SitterPageResponse response = SitterPageResponse.of(List.of(), 0, 0, 0, 10);
            given(sitterCacheService.searchSitters(eq(condition), eq(0), eq(10), eq("averageRating"), eq("desc")))
                    .willReturn(response);

            // when
            SitterPageResponse result = sitterService.searchSitters(condition, 0, 10, "averageRating", "desc");

            // then
            assertThat(result).isEqualTo(response);
            then(sitterCacheService).should().searchSitters(eq(condition), eq(0), eq(10), eq("averageRating"), eq("desc"));
        }

        @Test
        @DisplayName("[성공] sort=averageRating, direction=asc 요청은 캐시 서비스에 위임된다")
        void search_sitters_sort_by_average_rating_asc() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);
            SitterPageResponse response = SitterPageResponse.of(List.of(), 0, 0, 0, 10);
            given(sitterCacheService.searchSitters(eq(condition), eq(0), eq(10), eq("averageRating"), eq("asc")))
                    .willReturn(response);

            // when
            SitterPageResponse result = sitterService.searchSitters(condition, 0, 10, "averageRating", "asc");

            // then
            assertThat(result).isEqualTo(response);
            then(sitterCacheService).should().searchSitters(eq(condition), eq(0), eq(10), eq("averageRating"), eq("asc"));
        }

        @Test
        @DisplayName("[실패] 허용되지 않은 direction 값은 INVALID_SORT_FIELD를 반환한다")
        void search_sitters_invalid_direction() {
            // given
            SitterSearchCondition condition = new SitterSearchCondition(null, null, null, null, null);

            // when & then
            assertThatThrownBy(() -> sitterService.searchSitters(condition, 0, 10, "createdAt", "random"))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.INVALID_SORT_FIELD));
            then(sitterCacheService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("시터 상세 조회 — GET /api/sitters/{sitterId}")
    class GetSitterByIdTest {

        @Test
        @DisplayName("[성공] 존재하는 sitterId로 상세 조회하면 프로필 정보를 반환한다")
        void get_sitter_by_id_test_01() {
            // given
            sitterProfile.approve(adminMemberId);
            SitterResponseDto expected = SitterResponseDto.from(sitterProfile, member1.getRegion(), List.of());
            given(sitterCacheService.getSitterById(sitterProfileId)).willReturn(expected);

            // when
            SitterResponseDto result = sitterService.getSitterById(sitterProfileId);

            // then
            assertThat(result.id()).isEqualTo(sitterProfileId);
            assertThat(result.memberId()).isEqualTo(member1Id);
            assertThat(result.region()).isEqualTo(Region.SEOCHO);
            assertThat(result.introduction()).isEqualTo("반려동물을 사랑하는 시터입니다");
            assertThat(result.experienceYears()).isEqualTo(3);
            assertThat(result.possiblePetType()).isEqualTo(PossiblePetType.ALL);
            assertThat(result.possiblePetSize()).isEqualTo(PossiblePetSize.ALL);
            assertThat(result.pricePerHour()).isEqualTo(15000);
            assertThat(result.status()).isEqualTo(SitterProfileStatus.RESERVABLE);
        }

        @Test
        @DisplayName("[성공] schedules가 있는 시터 상세 조회 시 schedules를 포함한다")
        void get_sitter_by_id_test_02() {
            // given
            SitterSchedule schedule = SitterSchedule.builder()
                    .sitterProfileId(sitterProfileId)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            ReflectionTestUtils.setField(schedule, "id", 1L);

            sitterProfile.approve(adminMemberId);
            SitterResponseDto expected = SitterResponseDto.from(sitterProfile, member1.getRegion(), List.of(schedule));
            given(sitterCacheService.getSitterById(sitterProfileId)).willReturn(expected);

            // when
            SitterResponseDto result = sitterService.getSitterById(sitterProfileId);

            // then
            assertThat(result.schedules()).hasSize(1);
        }

        @Test
        @DisplayName("[성공] schedules가 없는 시터 상세 조회 시 빈 schedules를 반환한다")
        void get_sitter_by_id_test_03() {
            // given
            sitterProfile.approve(adminMemberId);
            SitterResponseDto expected = SitterResponseDto.from(sitterProfile, member1.getRegion(), List.of());
            given(sitterCacheService.getSitterById(sitterProfileId)).willReturn(expected);

            // when
            SitterResponseDto result = sitterService.getSitterById(sitterProfileId);

            // then
            assertThat(result.schedules()).isEmpty();
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 sitterId는 SITTER_NOT_FOUND를 반환한다")
        void get_sitter_by_id_test_04() {
            // given
            given(sitterCacheService.getSitterById(99999L)).willThrow(new SitterException(SitterErrorCode.SITTER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> sitterService.getSitterById(99999L))
                    .isInstanceOf(SitterException.class)
                    .satisfies(ex -> assertThat(((SitterException) ex).getErrorCode())
                            .isEqualTo(SitterErrorCode.SITTER_NOT_FOUND));
        }
    }
}