package com.forpets.domain.reservation.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.proposal.entity.Proposal;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.dto.CancelReservationRequest;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.*;
import com.forpets.domain.reservation.exception.ReservationErrorCode;
import com.forpets.domain.reservation.exception.ReservationException;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import com.forpets.global.embed.HasTimeSlotInfo;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final int DEPOSIT_RATIO = 20;

    private final ReservationRepository reservationRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;

    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;

    private final TimeSlotValidator timeSlotValidator;

    /*
    예약 생성 1: 순방향로직에서 Reservation 생성 (트리거: CareRequest 수락)
     */
    @Transactional
    public void createFromCareRequest(CareRequest careRequest, Long sitterMemberId,
                                             List<CareRequestPet> crPets,
                                             List<CareRequestTimeSlot> crTimeSlots) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .guardianId(careRequest.getMemberId())
                .sitterMemberId(sitterMemberId)
                .sitterProfileId(careRequest.getSitterProfileId())
                .careType(careRequest.getCareType())
                .source(ReservationSource.CARE_REQUEST)
                .sourceId(careRequest.getId())
                .build());

        // PetSnapshot 복사
        crPets.forEach(crPet -> reservationPetRepository.save(
                ReservationPet.createFrom(reservation.getId(), crPet.getPetId(), crPet.getPetSnapshot())));

        // TimeSlot 복사
        crTimeSlots.forEach(crSlot -> reservationTimeSlotRepository.save(
                ReservationTimeSlot.create(reservation.getId(), crSlot.getTimeSlotInfo())));

        // Payment 생성
        reservationPaymentRepository.save(ReservationPayment.create(
                reservation.getId(),
                careRequest.getRequestPrice(),
                (careRequest.getRequestPrice() * DEPOSIT_RATIO) / 100 ));

        // 나중에 return 값 써야하면 쓰기 .... Kafka 같은 곳에서!
//        return reservation;
    }

    /*
    예약 생성2: 역방향 매칭 (트리거: proposal 수락)
     */
    @Transactional
    public void createFromProposal(Proposal proposal, Post post, Long sitterMemberId,
                                          List<PostPet> postPets,
                                          List<PostTimeSlot> postTimeSlots) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .guardianId(post.getMemberId())
                .sitterMemberId(sitterMemberId)
                .sitterProfileId(proposal.getSitterProfileId())
                .careType(post.getCareType())
                .source(ReservationSource.PROPOSAL)
                .sourceId(proposal.getId())
                .build());

        // PetSnapshot 복사
        postPets.forEach(pp -> reservationPetRepository.save(
                ReservationPet.createFrom(reservation.getId(), pp.getPetId(), pp.getPetSnapshot())));

        // TimeSlot 복사
        postTimeSlots.forEach(pts -> reservationTimeSlotRepository.save(
                ReservationTimeSlot.create(reservation.getId(), pts.getTimeSlotInfo())));

        // Payment 생성
        reservationPaymentRepository.save(ReservationPayment.create(
                reservation.getId(),
                proposal.getProposedPrice(),
                (proposal.getProposedPrice() * DEPOSIT_RATIO) / 100 ));

//        return reservation;
    }


    /*
    내 예약 목록 조회
     */
    public List<ReservationResponseDto> getMyReservations(Long memberId) {
        List<Reservation> asGuardian = reservationRepository.findAllByGuardianId(memberId);
        List<Reservation> asSitter = reservationRepository.findAllBySitterMemberId(memberId);

        List<Reservation> all = new ArrayList<>(asGuardian);
        all.addAll(asSitter);

        return all.stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .map(this::toResponseDto)
                .toList();
    }

    /*
    예약 상세 조회
    예약 당사자여야 가능
     */
    public ReservationResponseDto getDetail(Long memberId, Long reservationId) {
        Reservation reservation = findById(reservationId);
        validateParty(memberId, reservation);
        return toResponseDto(reservation);
    }

    /*
    예약 확정 요청
    보호자가 호출하면 guardianPaid = true
    시터가 호출하면 sitterPaid = true
    양쪽 다 true면 CONFIRMED로 전환 + 후속 처리

    CONFIRMED 전환 시:
    1. 같은 시터의 겹치는 시간대 CONFIRMED 예약 충돌 검사
    2. 충돌 없으면 CONFIRMED 전환
    3. 같은 공고의 나머지 PENDING 제안 → REJECTED (Proposal 출처인 경우)
    4. 같은 시터의 겹치는 시간대 Proposal → WITHDRAWN
    5. 공고 → CLOSED (Proposal 출처인 경우)
     */
    @Transactional
    public ReservationResponseDto confirm(Long memberId, Long reservationId) {

        /*
        V2 에서는 여기 결제 로직이 추가되어야합니다!!!!!!!!
         */

        Reservation reservation = findById(reservationId);
        validateParty(memberId, reservation);
        validatePending(reservation);

        ReservationPayment payment = findPayment(reservationId);

        // 보호자/시터 결제 확인 처리
        if (reservation.isGuardian(memberId)) {
            if (payment.isGuardianPaid()) {
                log.info("[ReservationService] 보호자 결제 완료 멱등 처리: 중복 요청 무시");
                // 이 부분 결제할 때 PaymentException 으로 바꾸는게 좋아보이긴 합니다!
                // 아래도 마찬가지. . .
                throw new ReservationException(ReservationErrorCode.ALREADY_PAID);
            } else {
                payment.guardianConfirm();
                log.info("[예약 확정] reservationId={}, 보호자(memberId={}) 결제 확인", reservationId, memberId);
            }
        } else {
            if (payment.isSitterPaid()) {
                log.info("[ReservationService] 시터 결제 완료 멱등 처리: 중복 요청 무시");
                throw new ReservationException(ReservationErrorCode.ALREADY_PAID);
            } else {
                payment.sitterConfirm();
                log.info("[예약 확정] reservationId={}, 시터(memberId={}) 결제 확인", reservationId, memberId);
            }
        }

        // 양쪽 다 결제 완료 시 CONFIRMED 전환
        if (payment.isBothPaid()) {
            validateNoConfirmedConflict(reservation);
            reservation.confirm();
            log.info("[예약 확정] reservationId={}, 양측 결제 완료 → CONFIRMED", reservationId);

            // 후속 처리
            handlePostConfirmation(reservation);
        }

        return toResponseDto(reservation);
    }


    /*
    케어 완료 처리
    시터만 호출 가능, CONFIRMED 상태에서만 가능
     */
    @Transactional
    public ReservationResponseDto complete(Long memberId, Long reservationId) {
        /*
        시스템이 보관하고 있던 돈을 시터에게 보내주는 로직
         */
        Reservation reservation = findById(reservationId);
        validateSitter(memberId, reservation);
        validateConfirmed(reservation);
        validateCareCompleted(reservationId);

        reservation.complete();
        log.info("[케어 완료] reservationId={}, 시터(memberId={}) 완료 처리", reservationId, memberId);

        return toResponseDto(reservation);
    }

    /*
    예약 취소
    PENDING 또는 CONFIRMED 상태만 취소 가능
    예약 당사자만 취소 가능
    취소 사유 필수 (최소 10자)
     */
    @Transactional
    public ReservationResponseDto cancel(Long memberId, Long reservationId, CancelReservationRequest request) {
        Reservation reservation = findById(reservationId);
        validateParty(memberId, reservation);
        validateCancelable(reservation);

        CanceledBy canceledBy = reservation.isGuardian(memberId) ? CanceledBy.GUARDIAN : CanceledBy.SITTER;

        reservation.cancel(request.cancelReason(), request.cancelCategory(), canceledBy);
        log.info("[예약 취소] reservationId={}, 취소 주체={}, 사유={}", reservationId, canceledBy, request.cancelReason());

        // 환불할 예약금이 있으면 환불 처리
        // V2 결제 연동 시 환불 로직 구현
        // ReservationPayment payment = findPayment(reservationId);
        // if (payment.isGuardianPaid()) refundGuardian(reservation);
        // if (payment.isSitterPaid()) refundSitter(reservation);

        // Proposal 출처인 경우: ACCEPTED → PENDING 복원, 공고 OPEN 유지
        handlePostCancellation(reservation);

        return toResponseDto(reservation);
    }



    // transaction 아닌 애들 ==========

    private void validateParty(Long memberId, Reservation reservation) {
        if (!reservation.isParty(memberId)) {
            log.info("현재 로그인 한 멤버 Id: {}, reservation 연관 memberId: {}, {}", memberId, reservation.getGuardianId(), reservation.getSitterMemberId());
            throw new ReservationException(ReservationErrorCode.NOT_RESERVATION_PARTY);
        }
    }

    private void validateSitter(Long memberId, Reservation reservation) {
        if (!reservation.isSitter(memberId)) {
            throw new ReservationException(ReservationErrorCode.NOT_RESERVATION_SITTER);
        }
    }

    public Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    // reservation 을 ResponseDto 로 변환하는 메서드
    // payment 정보 + pet snapshot 정보 + timeSLot 정보... 다 해서 from method 로 넘기기
    private ReservationResponseDto toResponseDto(Reservation reservation) {
        ReservationPayment payment = findPayment(reservation.getId());
        List<ReservationPet> pets = reservationPetRepository.findAllByReservationId(reservation.getId());
        List<ReservationTimeSlot> timeSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservation.getId());
        return ReservationResponseDto.from(reservation, payment, pets, timeSlots);
    }

    // payment 찾기
    private ReservationPayment findPayment(Long reservationId) {
        return reservationPaymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    public boolean hasConfirmedConflict(Long sitterProfileId, List<? extends HasTimeSlotInfo> timeSlots) {
        List<Reservation> confirmed = reservationRepository
                .findAllBySitterProfileIdAndStatus(sitterProfileId, ReservationStatus.CONFIRMED);

        return hasConflictWith(confirmed, timeSlots);
    }

    /*
    V2
    PENDING 예약과 시간 충돌 여부 확인
    충돌 시 true 반환 → 호출하는 쪽에서 경고 처리
     */
//    public boolean hasPendingConflict(Long sitterProfileId, List<? extends HasTimeSlotInfo> timeSlots) {
//        List<Reservation> pending = reservationRepository
//                .findAllBySitterProfileIdAndStatus(sitterProfileId, ReservationStatus.PENDING);
//
//        return hasConflictWith(pending, timeSlots);
//    }


    private boolean hasConflictWith(List<Reservation> reservations, List<? extends HasTimeSlotInfo> newSlots) {
        for (Reservation existing : reservations) {
            List<ReservationTimeSlot> existingSlots = reservationTimeSlotRepository
                    .findAllByReservationIdOrderByTimeSlotInfoSequence(existing.getId());

            if (timeSlotValidator.hasTimeConflict(existingSlots, newSlots)) {
                return true;
            }
        }
        return false;
    }

    private void validatePending(Reservation reservation) {
        if (!reservation.isPending()) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
    }

    private void validateCancelable(Reservation reservation) {
        if (!reservation.isCancelable()) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
    }

    private void validateConfirmed(Reservation reservation) {
        if (!reservation.isConfirmed()) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVATION_STATUS_TRANSITION);
        }
    }

    /*
    CONFIRMED 예약 시간 충돌 검사
    같은 시터의 CONFIRMED 예약 중 시간이 겹치는 건이 있으면 차단
     */
    private void validateNoConfirmedConflict(Reservation reservation) {
        List<Reservation> confirmedReservations = reservationRepository
                .findAllBySitterProfileIdAndStatus(reservation.getSitterProfileId(), ReservationStatus.CONFIRMED);

        List<ReservationTimeSlot> newTimeSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservation.getId());

        for (Reservation existing : confirmedReservations) {
            List<ReservationTimeSlot> existingTimeSlots = reservationTimeSlotRepository
                    .findAllByReservationIdOrderByTimeSlotInfoSequence(existing.getId());

            if (timeSlotValidator.hasTimeConflict(newTimeSlots, existingTimeSlots)) {
                throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
            }
        }
    }

    /*
    CONFIRMED 후속 처리
    - Proposal 출처: 같은 공고의 나머지 PENDING 제안 → REJECTED, 공고 → CLOSED
    - 같은 시터의 겹치는 시간대 Proposal → WITHDRAWN
     */
    private void handlePostConfirmation(Reservation reservation) {
        if (reservation.getSource() == ReservationSource.PROPOSAL) {
            // Proposal 출처: 같은 공고의 나머지 PENDING 제안 REJECTED + 공고 CLOSED
            Proposal acceptedProposal = proposalRepository.findById(reservation.getSourceId()).orElse(null);
            if (acceptedProposal != null) {
                Long postId = acceptedProposal.getPostId();

                // 나머지 PENDING 제안 → REJECTED
                proposalRepository.findAllByPostIdAndStatus(postId, ProposalStatus.PENDING).stream()
                        .filter(p -> !p.getId().equals(acceptedProposal.getId()))
                        .forEach(Proposal::reject);

                // 공고 → CLOSED
                postRepository.findById(postId).ifPresent(Post::close);
            }
        }

        withdrawConflictingProposals(reservation);
        // 겹치는 CareRequest 는 따로 reject 처리 하지 않음
        // PENDING 유지 (수락 시 충돌 검증에서 차단)
    }

    /*
    같은 시터의 다른 공고에 보낸 PENDING Proposal 중
    CONFIRMED된 예약 시간과 겹치는 건 자동 WITHDRAWN
     */
    private void withdrawConflictingProposals(Reservation reservation) {
        List<ReservationTimeSlot> confirmedSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservation.getId());

        List<Proposal> pendingProposals = proposalRepository
                .findAllBySitterProfileIdAndStatus(reservation.getSitterProfileId(), ProposalStatus.PENDING);

        for (Proposal proposal : pendingProposals) {
            List<PostTimeSlot> postSlots = postTimeSlotRepository
                    .findAllByPostIdOrderByTimeSlotInfoSequence(proposal.getPostId());

            if (hasTimeConflictBetween(confirmedSlots, postSlots)) {
                proposal.withdraw();
            }
        }
    }

    private boolean hasTimeConflictBetween(List<ReservationTimeSlot> reservationSlots,
                                           List<PostTimeSlot> postSlots) {
        for (ReservationTimeSlot rs : reservationSlots) {
            TimeSlotInfo ri = rs.getTimeSlotInfo();
            for (PostTimeSlot ps : postSlots) {
                TimeSlotInfo pi = ps.getTimeSlotInfo();

                if (ri.getCareDate().equals(pi.getCareDate())
                        && ri.getStartTime().isBefore(pi.getEndTime())
                        && ri.getEndTime().isAfter(pi.getStartTime())) {
                    return true;
                }
            }
        }
        return false;
    }


    /*
    취소 후속 처리
    - Proposal 출처: ACCEPTED → PENDING 복원 (다른 제안 채택 가능)
     */
    private void handlePostCancellation(Reservation reservation) {
        if (reservation.getSource() == ReservationSource.PROPOSAL) {
            proposalRepository.findById(reservation.getSourceId()).ifPresent(Proposal::restoreToPending);
        }
    }

    private void validateCareCompleted(Long reservationId) {
        List<ReservationTimeSlot> timeSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservationId);

        if (timeSlots.isEmpty()) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        ReservationTimeSlot lastSlot = timeSlots.getLast();
        TimeSlotInfo info = lastSlot.getTimeSlotInfo();
        LocalDateTime careEndDateTime = LocalDateTime.of(info.getCareDate(), info.getEndTime());

        if (LocalDateTime.now().isBefore(careEndDateTime)) {
            throw new ReservationException(ReservationErrorCode.CARE_NOT_COMPLETED_YET);
        }
    }

    // ----- pet service 에서 쓸 ... method  -----

    public boolean existsActiveReservationByPetId(Long petId) {
        return reservationRepository.existsByPetIdAndStatusIn(
                petId,
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
        );
    }

    public boolean existsInProgressBySitterId(Long sitterId) {
        return reservationRepository.existsBySitterProfileIdAndStatus(sitterId, ReservationStatus.CONFIRMED);
    }
}
