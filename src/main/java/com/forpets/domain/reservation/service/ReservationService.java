package com.forpets.domain.reservation.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.post.repository.PostTimeSlotRepository;
import com.forpets.domain.payment.service.PaymentRefundService;
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
    private static final long PAYMENT_EXPIRE_HOURS = 2L;
    private static final String EXPIRE_REFUND_REASON = "예약 결제 제한 시간 초과";

    private final ReservationRepository reservationRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PaymentRefundService paymentRefundService;

    private final ReservationLockService reservationLockService;

    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;

    private final TimeSlotValidator timeSlotValidator;
    private final CareRequestRepository careRequestRepository;

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
    Payment 도메인에서 PortOne 결제 검증을 마친 뒤 호출하는 예약 확정 처리.
    양측 결제가 모두 완료된 경우에만 Reservation 을 CONFIRMED 로 전환한다.
     */
    @Transactional
    public ReservationResponseDto confirmAfterPayment(Long reservationId) {
        Reservation reservation = findById(reservationId);
        validatePending(reservation);

        ReservationPayment payment = findPayment(reservationId);
        // 한쪽만 결제한 상황이면 아직 PENDING 으로 둬야함
        if (!payment.isBothPaid()) {
            return toResponseDto(reservation);
        }

        return reservationLockService.executeWithSitterLock(
                reservation.getSitterProfileId(), () -> {
                    /*
                    CONFIRMED 예약 중 겹치는게 있으면 안 됨
                    Reservation Status = CONFIRMED 로 업데이트
                    역방향으로 인한 Reservation 인 경우 Post, Proposal 후속처리
                     */
                    validateNoConfirmedConflict(reservation);
                    reservation.confirm();
                    log.info("[예약 확정] reservationId={}, CONFIRMED", reservationId);
                    handlePostConfirmation(reservation);
                    return toResponseDto(reservation);
                });
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

        paymentRefundService.refundPaidPayments(reservationId, request.cancelReason());
        paymentRefundService.expireNonPaidPayments(reservationId);

        // Proposal 출처인 경우: ACCEPTED → PENDING 복원, 공고 OPEN 유지
        handlePostCancellation(reservation);

        return toResponseDto(reservation);
    }

    /*
    예약 만료 처리
    PENDING 예약이 생성 후 2시간을 넘기면 EXPIRED로 닫고 이미 결제된 금액을 환불한다.
     */
    @Transactional
    public int expirePendingReservations(LocalDateTime now) {
        LocalDateTime deadline = now.minusHours(PAYMENT_EXPIRE_HOURS);
        List<Reservation> expiredTargets = reservationRepository.findAllByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING, deadline);

        expiredTargets.forEach(this::expireReservation);

        if (!expiredTargets.isEmpty()) {
            log.info("[예약 만료] 처리 건수={}", expiredTargets.size());
        }

        return expiredTargets.size();
    }

    private void expireReservation(Reservation reservation) {
        reservation.expire();
        paymentRefundService.refundPaidPayments(reservation.getId(), EXPIRE_REFUND_REASON);
        paymentRefundService.expireNonPaidPayments(reservation.getId());
        handlePostCancellation(reservation);

        // careRequest 도 restore 해줌
        // 수동 취소에서도 Pending 으로 돌아가는걸 방지하기 위해 expireReservation 으로 위치 이동시킴
        if (reservation.getSource() == ReservationSource.CARE_REQUEST){
            careRequestRepository.findById(reservation.getSourceId()).ifPresent(CareRequest::restoreToPending);
        }
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

        // 보내뒀던 다른 Proposal 들 중 겹치는건 Withdraw 처리
        withdrawConflictingProposals(reservation);

        // 겹치는 CareRequest 는 따로 reject 처리 하지 않고
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

            if (timeSlotValidator.hasTimeConflict(confirmedSlots, postSlots)) {
                proposal.withdraw();
            }
        }
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

//    public boolean existsActiveReservationByPetId(Long petId) {
//        return reservationRepository.existsByPetIdAndStatusIn(
//                petId,
//                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED)
//        );
//    }

    public boolean existsInProgressBySitterId(Long sitterId) {
        return reservationRepository.existsBySitterProfileIdAndStatus(sitterId, ReservationStatus.CONFIRMED);
    }
}
