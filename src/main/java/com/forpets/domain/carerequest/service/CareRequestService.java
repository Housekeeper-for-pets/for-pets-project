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
import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.service.MemberService;
import com.forpets.domain.pet.entity.Pet;
import com.forpets.domain.pet.exception.PetErrorCode;
import com.forpets.domain.pet.exception.PetException;
import com.forpets.domain.pet.service.PetService;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.service.ReservationService;
import com.forpets.domain.sitter.entity.SitterProfile;
import com.forpets.domain.sitter.repository.SitterProfileRepository;
import com.forpets.domain.sitter.service.SitterService;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.dto.TimeSlotRequest;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CareRequestService {

    private final CareRequestRepository careRequestRepository;
    private final CareRequestPetRepository careRequestPetRepository;
    private final CareRequestTimeSlotRepository careRequestTimeSlotRepository;
    private final PetService petService;
    private final SitterService sitterService;
    private final TimeSlotValidator timeSlotValidator;
    private final ReservationService reservationService;

    /*
    케어 요청 등록
    1. 시터 RESERVABLE 상태 검증
    2. 본인에게 요청 방지
    3. 반려동물 존재 + 본인 소유 검증
    4. TimeSlotValidator 검증
    5. 동일 시터에게 동일 petIds로 PENDING 중복 요청 방지
    6. CareRequest + CareRequestPet(스냅샷) + CareRequestTimeSlot 생성
     */
    @Transactional
    public CareRequestResponseDto create(Long memberId, Long sitterId, CreateCareRequestDto request) {
        SitterProfile sitter = sitterService.findById(sitterId);
        validateReservable(sitter);

        log.info("로그인 한 유저의 member Id: {}", memberId);
        log.info("타겟 시터의 member Id: {}", sitter.getMemberId());

        validateNotSelf(memberId, sitter.getMemberId());

        List<Pet> pets = validateAndGetPets(memberId, request.petIds());
        timeSlotValidator.validate(request.timeSlots());
        validateNoDuplicatePendingRequest(sitter.getId(), request.petIds());

        CareRequest careRequest = careRequestRepository.save(CareRequest.builder()
                .memberId(memberId)
                .sitterProfileId(sitter.getId())
                .sitterMemberId(sitter.getMemberId())
                .careType(request.careType())
                .message(request.message())
                .requestPrice(request.requestPrice())
                .build());

        List<CareRequestPet> careRequestPets = saveCareRequestPets(careRequest.getId(), pets);
        List<CareRequestTimeSlot> careRequestTimeSlots = saveCareRequestTimeSlots(careRequest.getId(), request.timeSlots());

        return CareRequestResponseDto.from(careRequest, careRequestPets, careRequestTimeSlots);
    }

    /*
    보낸 요청 목록 조회
     */
    public List<CareRequestResponseDto> getSentRequests(Long memberId) {
        return careRequestRepository.findAllByMemberId(memberId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    /*
    보낸 요청 상세 조회
    본인이 보냈거나 본인이 받아야 조회 가능
     */
    public CareRequestResponseDto getDetail(Long memberId, Long requestId) {
        CareRequest request = findById(requestId);
        validateParty(memberId, request);
        return toResponseDto(request);
    }

    /*
    보낸 요청 취소
    PENDING 상태만 취소 가능 - 수락시 바로 reservation 이 만들어지니까
     */
    @Transactional
    public CareRequestResponseDto cancel(Long memberId, Long requestId) {
        CareRequest request = findById(requestId);
        validateOwner(memberId, request);
        validatePending(request);

        request.cancel();
        return toResponseDto(request);
    }

    /*
    받은 요청 목록 조회
    시터 본인에게 온 요청만 조회
     */
    public List<CareRequestResponseDto> getReceivedRequests(Long memberId) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);

        return careRequestRepository.findAllBySitterProfileId(sitter.getId()).stream()
                .map(this::toResponseDto)
                .toList();
    }

    /*
    요청 수락
    1. PENDING 상태 검증
    2. 요청 대상 시터 본인 검증
    3. 예약 충돌 검증
        - CONFIRMED 예약이 있으면 수락 불가능
        - PENDING 예약이 있으면 경고 메시지
    4. CareRequest → ACCEPTED
    5. Reservation 자동 생성 (PENDING)
       - CareRequestPet → ReservationPet 스냅샷 복사
       - CareRequestTimeSlot → ReservationTimeSlot 복사
     */
    @Transactional
    public CareRequestResponseDto accept(Long memberId, Long requestId) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);
        CareRequest request = findById(requestId);
        validatePending(request);
        validateTargetSitter(sitter.getId(), request);

        // CONFIRMED 예약 충돌 검증
         List<CareRequestTimeSlot> timeSlots =
             careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(request.getId());
         if (reservationService.hasConfirmedConflict(sitter.getId(), timeSlots)) {
             throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
         }
         //if (rservationService.hasPendingConflict(sitter.getId, timeSlots)) { }
         // V2: 경고 메시지 띄우기 근데 수락이 가능하긴 함 한 번 더 물어보기

        request.accept();

//       log.info("[CareRequestService] PENDING 상태의 예약 생성");
//       Reservation 자동 생성: 주체가 Sitter
         List<CareRequestPet> crPets = careRequestPetRepository.findAllByCareRequestId(request.getId());
         List<CareRequestTimeSlot> crTimeSlots =
             careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(request.getId());
         reservationService.createFromCareRequest(request, memberId, crPets, crTimeSlots);
        return toResponseDto(request);
    }

    /*
    요청 거절
    PENDING 상태만 거절 가능
    요청 대상 시터 본인만 거절 가능
     */
    @Transactional
    public CareRequestResponseDto reject(Long memberId, Long requestId) {
        SitterProfile sitter = sitterService.findByMemberId(memberId);
        CareRequest request = findById(requestId);
        validatePending(request);
        validateTargetSitter(sitter.getId(), request);

        request.reject();
        return toResponseDto(request);
    }

    // ===== private methods =====

    public CareRequest findById(Long requestId) {
        return careRequestRepository.findById(requestId)
                .orElseThrow(() -> new CareRequestException(CareRequestErrorCode.CARE_REQUEST_NOT_FOUND));
    }

    private void validateParty(Long memberId, CareRequest careRequest) {
        if (!careRequest.isOwnedBy(memberId) && !careRequest.getSitterMemberId().equals(memberId)) {
            throw new CareRequestException(CareRequestErrorCode.NOT_CARE_REQUEST_PARTY);
        }
    }

    private void validateReservable(SitterProfile sitter) {
        if (!sitter.isReservable()) {
            throw new CareRequestException(CareRequestErrorCode.SITTER_NOT_RESERVABLE);
        }
    }

    private void validateNotSelf(Long memberId, Long targetSitterId) {
        if (targetSitterId.equals(memberId)) {
            throw new CareRequestException(CareRequestErrorCode.CANNOT_REQUEST_TO_SELF);
        }
    }

    private void validateOwner(Long memberId, CareRequest request) {
        if (!request.isOwnedBy(memberId)) {
            throw new CareRequestException(CareRequestErrorCode.NOT_CARE_REQUEST_OWNER);
        }
    }

    private void validateTargetSitter(Long sitterProfileId, CareRequest request) {
        if (!request.isTargetSitter(sitterProfileId)) {
            throw new CareRequestException(CareRequestErrorCode.NOT_TARGET_SITTER);
        }
    }

    private void validatePending(CareRequest request) {
        if (!request.isPending()) {
            throw new CareRequestException(CareRequestErrorCode.NOT_PENDING_CARE_REQUEST);
        }
    }

    private List<Pet> validateAndGetPets(Long memberId, List<Long> petIds) {
        return petIds.stream()
                .map(petId -> {
                    Pet pet = petService.findById(petId);
                    if (!pet.getMemberId().equals(memberId)) {
                        throw new PetException(PetErrorCode.NOT_PET_OWNER);
                    }
                    return pet;
                })
                .toList();
    }

    /*
    동일 시터에게 동일 petIds로 PENDING 중복 요청 방지
    기존 PENDING 요청의 petIds와 새 요청의 petIds를 정렬 후 비교
     */
    private void validateNoDuplicatePendingRequest(Long sitterProfileId, List<Long> petIds) {
        List<Long> sortedNewPetIds = petIds.stream().sorted().toList();

        List<CareRequest> pendingRequests =
                careRequestRepository.findAllBySitterProfileIdAndStatus(sitterProfileId, CareRequestStatus.PENDING);

        for (CareRequest existing : pendingRequests) {
            List<Long> existingPetIds = careRequestPetRepository.findAllByCareRequestId(existing.getId()).stream()
                    .map(CareRequestPet::getPetId)
                    .sorted()
                    .toList();

            if (existingPetIds.equals(sortedNewPetIds)) {
                throw new CareRequestException(CareRequestErrorCode.DUPLICATE_PENDING_REQUEST);
            }
        }
    }

    private List<CareRequestPet> saveCareRequestPets(Long careRequestId, List<Pet> pets) {
        List<CareRequestPet> crPets = pets.stream()
                .map(pet -> CareRequestPet.createFrom(careRequestId, pet))
                .toList();
        return careRequestPetRepository.saveAll(crPets);
    }

    /*
    CareRequestTimeSlot 저장 + sequence 자동 계산
    정렬: careDate ASC → startTime ASC
    sequence: 1부터 순차 할당
     */
    private List<CareRequestTimeSlot> saveCareRequestTimeSlots(Long careRequestId, List<TimeSlotRequest> timeSlots) {
        List<TimeSlotRequest> sorted = timeSlots.stream()
                .sorted(Comparator.comparing(TimeSlotRequest::careDate)
                        .thenComparing(TimeSlotRequest::startTime))
                .toList();

        List<CareRequestTimeSlot> crTimeSlots = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            TimeSlotRequest slot = sorted.get(i);
            TimeSlotInfo info = TimeSlotInfo.of(
                    slot.careDate(), slot.startTime(), slot.endTime(), i + 1);
            crTimeSlots.add(CareRequestTimeSlot.create(careRequestId, info));
        }

        return careRequestTimeSlotRepository.saveAll(crTimeSlots);
    }

    /*
    CareRequest → ResponseDto 변환 헬퍼
     */
    private CareRequestResponseDto toResponseDto(CareRequest request) {
        List<CareRequestPet> pets = careRequestPetRepository.findAllByCareRequestId(request.getId());
        List<CareRequestTimeSlot> timeSlots =
                careRequestTimeSlotRepository.findAllByCareRequestIdOrderByTimeSlotInfoSequence(request.getId());
        return CareRequestResponseDto.from(request, pets, timeSlots);
    }

    public boolean existsActiveCareRequestByPetId(Long petId) {
        return careRequestRepository.existsByPetIdAndStatusIn(
                petId,
                List.of(CareRequestStatus.PENDING, CareRequestStatus.ACCEPTED)
        );
    }
}
