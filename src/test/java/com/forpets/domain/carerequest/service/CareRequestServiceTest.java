package com.forpets.domain.carerequest.service;

import com.forpets.domain.carerequest.dto.CareRequestResponseDto;
import com.forpets.domain.carerequest.dto.CreateCareRequestDto;
import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.carerequest.exception.CareRequestErrorCode;
import com.forpets.domain.carerequest.exception.CareRequestException;
import com.forpets.domain.carerequest.repository.CareRequestPetRepository;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.carerequest.repository.CareRequestTimeSlotRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.entity.PetGender;
import com.forpets.domain.pet.entity.PetSize;
import com.forpets.domain.pet.entity.PetSpecies;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.domain.sitter.entity.PossiblePetSize;
import com.forpets.domain.sitter.entity.PossiblePetType;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.entity.SitterProfileStatus;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CareRequestServiceTest {

    @InjectMocks
    private CareRequestService careRequestService;

    @Mock
    private CareRequestRepository careRequestRepository;

    @Mock
    private CareRequestPetRepository careRequestPetRepository;

    @Mock
    private CareRequestTimeSlotRepository careRequestTimeSlotRepository;

    @Mock
    private PetService petService;

    @Mock
    private SitterService sitterService;

    @Mock
    private TimeSlotValidator timeSlotValidator;

    @Mock
    private ReservationService reservationService;

    @Mock
    private NotificationMessageBroker notificationBroker;

    // ── 테스트 픽스처 ──
    private SitterProfile sitterProfile;     // member2의 시터 프로필
    private Pet pet1;                        // member1(째길중)의 타코
    private Pet pet2;                        // member1의 연두
    private CareRequest careRequest;         // PENDING 상태 요청

    private final Long member1Id = 1L;       // 째길중 — 보호자
    private final Long member2Id = 2L;       // 타코맘 — 시터
    private final Long member3Id = 3L;       // 지민냥 — 제3자
    private final Long sitterProfileId = 100L;
    private final Long pet1Id = 10L;
    private final Long pet2Id = 11L;
    private final Long careRequestId = 200L;

    @BeforeEach
    void setUp() {
        // member2(타코맘)의 시터 프로필 — RESERVABLE
        sitterProfile = SitterProfile.builder()
                .memberId(member2Id)
                .introduction("고양이 전문 시터")
                .experienceYears(3)
                .possiblePetType(PossiblePetType.CAT)
                .possiblePetSize(PossiblePetSize.ALL)
                .pricePerHour(15000)
                .build();
        ReflectionTestUtils.setField(sitterProfile, "id", sitterProfileId);
        sitterProfile.approve(member3Id);

        // member1(째길중)의 반려동물들
        pet1 = Pet.builder()
                .memberId(member1Id)
                .name("타코")
                .species(PetSpecies.CAT)
                .breed("코리안숏헤어")
                .size(PetSize.SMALL)
                .age(4)
                .gender(PetGender.MALE)
                .note("길냥이에요, 많이 울어요, 귀여워요")
                .build();
        ReflectionTestUtils.setField(pet1, "id", pet1Id);

        pet2 = Pet.builder()
                .memberId(member1Id)
                .name("연두")
                .species(PetSpecies.DOG)
                .breed("포메라니안")
                .size(PetSize.SMALL)
                .age(1)
                .gender(PetGender.FEMALE)
                .note("굉장히 귀여움 겨우 2.5kg")
                .build();
        ReflectionTestUtils.setField(pet2, "id", pet2Id);

        // PENDING 상태 CareRequest
        careRequest = CareRequest.builder()
                .memberId(member1Id)
                .sitterProfileId(sitterProfileId)
                .sitterMemberId(member2Id)
                .careType(CareType.VISIT)
                .message("타코 돌봐주세요")
                .requestPrice(30000)
                .build();
        ReflectionTestUtils.setField(careRequest, "id", careRequestId);
    }

    // ── 헬퍼 ──
    private List<TimeSlotRequest> validTimeSlots() {
        return List.of(
                new TimeSlotRequest(
                        LocalDate.now().plusDays(3),
                        LocalTime.of(10, 0),
                        LocalTime.of(14, 0)
                )
        );
    }

    private CareRequestPet createCareRequestPet(Long crId, Pet pet) {
        CareRequestPet crPet = CareRequestPet.createFrom(crId, pet);
        ReflectionTestUtils.setField(crPet, "id", pet.getId() + 100);
        return crPet;
    }

    private CareRequestTimeSlot createCareRequestTimeSlot(Long crId, int sequence) {
        TimeSlotInfo info = TimeSlotInfo.of(
                LocalDate.now().plusDays(3), LocalTime.of(10, 0), LocalTime.of(14, 0), sequence);
        CareRequestTimeSlot slot = CareRequestTimeSlot.create(crId, info);
        ReflectionTestUtils.setField(slot, "id", (long) (sequence + 300));
        return slot;
    }

    private void stubToResponseDto() {
        List<CareRequestPet> pets = List.of(createCareRequestPet(careRequestId, pet1));
        List<CareRequestTimeSlot> slots = List.of(createCareRequestTimeSlot(careRequestId, 1));
        given(careRequestPetRepository.findAllByCareRequestId(careRequestId)).willReturn(pets);
        given(careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(careRequestId))
                .willReturn(slots);
    }

    // ========================================================
    // 케어 요청 등록 — POST /api/care-requests/sitters/{sitterId}
    // ========================================================
    @Nested
    @DisplayName("케어 요청 등록 — POST /api/care-requests/sitters/{sitterId}")
    class CreateCareRequestTest {

        @Test
        @DisplayName("[성공] 보호자가 RESERVABLE 시터에게 케어 요청 성공")
        void carerequest_test_01() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(pet1Id), CareType.VISIT, "타코 돌봐주세요",
                    validTimeSlots(), 30000
            );
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(petService.validateAndGetPets(member1Id, List.of(pet1Id))).willReturn(List.of(pet1));
            willDoNothing().given(timeSlotValidator).validate(anyList());
            given(careRequestRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, CareRequestStatus.PENDING))
                    .willReturn(List.of());
            given(careRequestRepository.save(any(CareRequest.class))).willReturn(careRequest);
            given(careRequestPetRepository.saveAll(anyList())).willReturn(List.of(createCareRequestPet(careRequestId, pet1)));
            given(careRequestTimeSlotRepository.saveAll(anyList())).willReturn(List.of(createCareRequestTimeSlot(careRequestId, 1)));

            // when
            CareRequestResponseDto result = careRequestService.create(member1Id, sitterProfileId, request);

            // then
            assertThat(result.memberId()).isEqualTo(member1Id);
            assertThat(result.sitterProfileId()).isEqualTo(sitterProfileId);
            assertThat(result.careType()).isEqualTo(CareType.VISIT);
            assertThat(result.status()).isEqualTo(CareRequestStatus.PENDING);
            assertThat(result.requestPrice()).isEqualTo(30000);
            assertThat(result.pets()).hasSize(1);
            assertThat(result.timeSlots()).hasSize(1);
            then(careRequestRepository).should().save(any(CareRequest.class));
        }

        @Test
        @DisplayName("[성공] petIds가 다르면 동일 시터에게 중복 요청 가능")
        void carerequest_test_02() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(pet2Id), CareType.VISIT, "연두 돌봐주세요",
                    validTimeSlots(), 25000
            );

            // 기존 PENDING 요청에는 pet1Id만 존재
            CareRequest existingPending = CareRequest.builder()
                    .memberId(member1Id)
                    .sitterProfileId(sitterProfileId)
                    .sitterMemberId(member2Id)
                    .careType(CareType.VISIT)
                    .message("기존 요청")
                    .requestPrice(30000)
                    .build();
            ReflectionTestUtils.setField(existingPending, "id", 999L);

            CareRequestPet existingPet = createCareRequestPet(999L, pet1);

            CareRequest savedRequest = CareRequest.builder()
                    .memberId(member1Id)
                    .sitterProfileId(sitterProfileId)
                    .careType(CareType.VISIT)
                    .message("연두 돌봐주세요")
                    .requestPrice(25000)
                    .build();
            ReflectionTestUtils.setField(savedRequest, "id", 201L);

            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(petService.validateAndGetPets(member1Id, List.of(pet2Id))).willReturn(List.of(pet2));
            willDoNothing().given(timeSlotValidator).validate(anyList());
            given(careRequestRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, CareRequestStatus.PENDING))
                    .willReturn(List.of(existingPending));
            given(careRequestPetRepository.findAllByCareRequestId(999L)).willReturn(List.of(existingPet));
            given(careRequestRepository.save(any(CareRequest.class))).willReturn(savedRequest);
            given(careRequestPetRepository.saveAll(anyList())).willReturn(List.of(createCareRequestPet(201L, pet2)));
            given(careRequestTimeSlotRepository.saveAll(anyList())).willReturn(List.of(createCareRequestTimeSlot(201L, 1)));

            // when
            CareRequestResponseDto result = careRequestService.create(member1Id, sitterProfileId, request);

            // then
            assertThat(result.sitterProfileId()).isEqualTo(sitterProfileId);
            then(careRequestRepository).should().save(any(CareRequest.class));
        }

        @Test
        @DisplayName("[실패] NON_RESERVABLE 시터에게 요청 시 차단")
        void carerequest_test_03() {
            // given
            sitterProfile.changeStatus(SitterProfileStatus.NON_RESERVABLE);
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(pet1Id), CareType.VISIT, "요청", validTimeSlots(), 30000
            );
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> careRequestService.create(member1Id, sitterProfileId, request))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.SITTER_NOT_RESERVABLE));
        }

        @Test
        @DisplayName("[실패] 본인(시터 본인)에게 요청 시 차단")
        void carerequest_test_04() {
            // given — member2가 본인 시터 프로필에 요청
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(pet1Id), CareType.VISIT, "요청", validTimeSlots(), 30000
            );
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);

            // when & then
            assertThatThrownBy(() -> careRequestService.create(member2Id, sitterProfileId, request))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.CANNOT_REQUEST_TO_SELF));
        }

        @Test
        @DisplayName("[실패] 타인 반려동물로 요청 시 차단")
        void carerequest_test_05() {
            // given — member3(지민냥)의 pet으로 요청
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(12L), CareType.VISIT, "요청", validTimeSlots(), 30000
            );
            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(petService.validateAndGetPets(member1Id, List.of(12L)))
                    .willThrow(new PetException(PetErrorCode.NOT_PET_OWNER));

            // when & then
            assertThatThrownBy(() -> careRequestService.create(member1Id, sitterProfileId, request))
                    .isInstanceOf(PetException.class)
                    .satisfies(ex -> assertThat(((PetException) ex).getErrorCode())
                            .isEqualTo(PetErrorCode.NOT_PET_OWNER));
        }

        @Test
        @DisplayName("[실패] 동일 시터 + 동일 petIds로 PENDING 중복 요청 시 차단")
        void carerequest_test_06() {
            // given
            CreateCareRequestDto request = new CreateCareRequestDto(
                    List.of(pet1Id), CareType.VISIT, "중복 요청", validTimeSlots(), 30000
            );
            CareRequest existingPending = CareRequest.builder()
                    .memberId(member1Id)
                    .sitterProfileId(sitterProfileId)
                    .sitterMemberId(member2Id)
                    .careType(CareType.VISIT)
                    .message("기존 요청")
                    .requestPrice(30000)
                    .build();
            ReflectionTestUtils.setField(existingPending, "id", 999L);

            CareRequestPet existingPet = createCareRequestPet(999L, pet1);

            given(sitterService.findApprovedById(sitterProfileId)).willReturn(sitterProfile);
            given(petService.validateAndGetPets(member1Id, List.of(pet1Id))).willReturn(List.of(pet1));
            willDoNothing().given(timeSlotValidator).validate(anyList());
            given(careRequestRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, CareRequestStatus.PENDING))
                    .willReturn(List.of(existingPending));
            given(careRequestPetRepository.findAllByCareRequestId(999L)).willReturn(List.of(existingPet));

            // when & then
            assertThatThrownBy(() -> careRequestService.create(member1Id, sitterProfileId, request))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.DUPLICATE_PENDING_REQUEST));
        }
    }

    // ========================================================
    // 보낸 요청 목록 조회 — GET /api/care-requests/sent
    // ========================================================
    @Nested
    @DisplayName("보낸 요청 목록 조회 — GET /api/care-requests/sent")
    class GetSentRequestsTest {

        @Test
        @DisplayName("[성공] 보낸 요청 목록 조회 성공")
        void carerequest_test_07() {
            // given
            given(careRequestRepository.findAllByMemberId(member1Id)).willReturn(List.of(careRequest));
            stubToResponseDto();

            // when
            List<CareRequestResponseDto> result = careRequestService.getSentRequests(member1Id);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).memberId()).isEqualTo(member1Id);
        }
    }

    // ========================================================
    // 보낸 요청 상세 조회 — GET /api/care-requests/{requestId}
    // ========================================================
    @Nested
    @DisplayName("보낸 요청 상세 조회 — GET /api/care-requests/{requestId}")
    class GetDetailTest {

        @Test
        @DisplayName("[성공] 요청자 본인이 상세 조회 성공")
        void carerequest_test_08() {
            // given
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));
            stubToResponseDto();

            // when
            CareRequestResponseDto result = careRequestService.getDetail(member1Id, careRequestId);

            // then
            assertThat(result.id()).isEqualTo(careRequestId);
            assertThat(result.memberId()).isEqualTo(member1Id);
        }

        @Test
        @DisplayName("[실패] 당사자가 아닌 회원이 상세 조회 시 차단")
        void carerequest_test_09() {
            // given
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.getDetail(member3Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_CARE_REQUEST_PARTY));
        }

        @Test
        @DisplayName("[실패] 존재하지 않는 요청 조회")
        void carerequest_test_10() {
            // given
            given(careRequestRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> careRequestService.getDetail(member1Id, 999L))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.CARE_REQUEST_NOT_FOUND));
        }
    }

    // ========================================================
    // 받은 요청 목록 조회 — GET /api/care-requests/received
    // ========================================================
    @Nested
    @DisplayName("받은 요청 목록 조회 — GET /api/care-requests/received")
    class GetReceivedRequestsTest {

        @Test
        @DisplayName("[성공] 시터가 받은 요청 목록 조회 성공")
        void carerequest_test_11() {
            // given
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findAllBySitterProfileId(sitterProfileId)).willReturn(List.of(careRequest));
            stubToResponseDto();

            // when
            List<CareRequestResponseDto> result = careRequestService.getReceivedRequests(member2Id);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).sitterProfileId()).isEqualTo(sitterProfileId);
        }
    }

    // ========================================================
    // 보낸 요청 취소 — PATCH /api/care-requests/{requestId}/cancel
    // ========================================================
    @Nested
    @DisplayName("보낸 요청 취소 — PATCH /api/care-requests/{requestId}/cancel")
    class CancelCareRequestTest {

        @Test
        @DisplayName("[성공] PENDING 상태 요청 취소 성공")
        void carerequest_test_12() {
            // given
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));
            stubToResponseDto();

            // when
            CareRequestResponseDto result = careRequestService.cancel(member1Id, careRequestId);

            // then
            assertThat(result.status()).isEqualTo(CareRequestStatus.CANCELED);
        }

        @Test
        @DisplayName("[실패] 본인 요청이 아닌 요청 취소 시 차단")
        void carerequest_test_13() {
            // given
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.cancel(member3Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_CARE_REQUEST_OWNER));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 요청 취소 시 차단")
        void carerequest_test_14() {
            // given
            careRequest.accept(); // ACCEPTED 상태로 변경
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.cancel(member1Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_PENDING_CARE_REQUEST));
        }
    }

    // ========================================================
    // 요청 수락 — PATCH /api/care-requests/{requestId}/accept
    // ========================================================
    @Nested
    @DisplayName("요청 수락 — PATCH /api/care-requests/{requestId}/accept")
    class AcceptCareRequestTest {

        @Test
        @DisplayName("[성공] 시터가 요청 수락 성공 — status ACCEPTED + Reservation 자동 생성")
        void carerequest_test_15() {
            // given
            List<CareRequestPet> crPets = List.of(createCareRequestPet(careRequestId, pet1));
            List<CareRequestTimeSlot> crSlots = List.of(createCareRequestTimeSlot(careRequestId, 1));

            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));
            given(careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(careRequestId))
                    .willReturn(crSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(false);
            given(careRequestPetRepository.findAllByCareRequestId(careRequestId)).willReturn(crPets);

            // when
            CareRequestResponseDto result = careRequestService.accept(member2Id, careRequestId);

            // then
            assertThat(result.status()).isEqualTo(CareRequestStatus.ACCEPTED);
            then(reservationService).should().createFromCareRequest(
                    eq(careRequest), eq(member2Id), eq(crPets), eq(crSlots));
        }

        @Test
        @DisplayName("[실패] 대상 시터가 아닌 시터가 수락 시도")
        void carerequest_test_16() {
            // given — member3은 다른 시터 프로필 소유
            SitterProfile otherSitter = SitterProfile.builder()
                    .memberId(member3Id)
                    .introduction("다른 시터")
                    .experienceYears(1)
                    .possiblePetType(PossiblePetType.DOG)
                    .possiblePetSize(PossiblePetSize.SMALL)
                    .pricePerHour(10000)
                    .build();
            ReflectionTestUtils.setField(otherSitter, "id", 101L);

            given(sitterService.findApprovedByMemberId(member3Id)).willReturn(otherSitter);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.accept(member3Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_TARGET_SITTER));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 요청 수락 시도")
        void carerequest_test_17() {
            // given
            careRequest.reject(); // REJECTED 상태로 변경
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.accept(member2Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_PENDING_CARE_REQUEST));
        }

        @Test
        @DisplayName("[실패] CONFIRMED 예약과 시간 충돌 시 수락 불가")
        void carerequest_test_18() {
            // given
            List<CareRequestTimeSlot> crSlots = List.of(createCareRequestTimeSlot(careRequestId, 1));

            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));
            given(careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(careRequestId))
                    .willReturn(crSlots);
            given(reservationService.hasConfirmedConflict(eq(sitterProfileId), anyList())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> careRequestService.accept(member2Id, careRequestId))
                    .isInstanceOf(ReservationException.class)
                    .satisfies(ex -> assertThat(((ReservationException) ex).getErrorCode())
                            .isEqualTo(ReservationErrorCode.RESERVATION_CONFLICT));
        }
    }

    // ========================================================
    // 요청 거절 — PATCH /api/care-requests/{requestId}/reject
    // ========================================================
    @Nested
    @DisplayName("요청 거절 — PATCH /api/care-requests/{requestId}/reject")
    class RejectCareRequestTest {

        @Test
        @DisplayName("[성공] 시터가 요청 거절 성공")
        void carerequest_test_19() {
            // given
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));
            stubToResponseDto();

            // when
            CareRequestResponseDto result = careRequestService.reject(member2Id, careRequestId);

            // then
            assertThat(result.status()).isEqualTo(CareRequestStatus.REJECTED);
        }

        @Test
        @DisplayName("[실패] 대상 시터가 아닌 시터가 거절 시도")
        void carerequest_test_20() {
            // given
            SitterProfile otherSitter = SitterProfile.builder()
                    .memberId(member3Id)
                    .introduction("다른 시터")
                    .experienceYears(1)
                    .possiblePetType(PossiblePetType.DOG)
                    .possiblePetSize(PossiblePetSize.SMALL)
                    .pricePerHour(10000)
                    .build();
            ReflectionTestUtils.setField(otherSitter, "id", 101L);

            given(sitterService.findApprovedByMemberId(member3Id)).willReturn(otherSitter);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.reject(member3Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_TARGET_SITTER));
        }

        @Test
        @DisplayName("[실패] PENDING이 아닌 요청 거절 시도")
        void carerequest_test_21() {
            // given
            careRequest.accept(); // ACCEPTED 상태로 변경
            given(sitterService.findApprovedByMemberId(member2Id)).willReturn(sitterProfile);
            given(careRequestRepository.findById(careRequestId)).willReturn(Optional.of(careRequest));

            // when & then
            assertThatThrownBy(() -> careRequestService.reject(member2Id, careRequestId))
                    .isInstanceOf(CareRequestException.class)
                    .satisfies(ex -> assertThat(((CareRequestException) ex).getErrorCode())
                            .isEqualTo(CareRequestErrorCode.NOT_PENDING_CARE_REQUEST));
        }
    }
}
