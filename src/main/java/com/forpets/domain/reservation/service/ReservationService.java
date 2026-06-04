package com.forpets.domain.reservation.service;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.notification.broker.NotificationMessageBroker;
import com.forpets.domain.notification.event.NotificationEvent;
import com.forpets.domain.payment.entity.Payment;
import com.forpets.domain.payment.entity.PaymentRole;
import com.forpets.domain.payment.entity.PaymentStatus;
import com.forpets.domain.payment.exception.PaymentErrorCode;
import com.forpets.domain.payment.exception.PaymentException;
import com.forpets.domain.payment.repository.PaymentRepository;
import com.forpets.domain.payment.service.PaymentRefundService;
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
import com.forpets.domain.settlement.service.SettlementService;
import com.forpets.global.aspect.DistributedLock;
import com.forpets.global.embed.HasTimeSlotInfo;
import com.forpets.global.embed.TimeSlotValidator;
import com.forpets.global.embed.entity.TimeSlotInfo;
import com.forpets.global.monitoring.TrackExecutionTime;
import com.forpets.global.sse.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private static final String NAME = ReservationService.class.getSimpleName();

    private static final int DEPOSIT_RATIO = 20;

    private final ReservationRepository reservationRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundService paymentRefundService;
    private final SettlementService settlementService;

    private final ReservationLockService reservationLockService;

    private final PostRepository postRepository;
    private final ProposalRepository proposalRepository;
    private final PostTimeSlotRepository postTimeSlotRepository;

    private final TimeSlotValidator timeSlotValidator;
    private final CareRequestRepository careRequestRepository;

    private final NotificationMessageBroker notificationBroker;

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
    public List<ReservationResponseDto> getMyReservations(Long memberId, ReservationRole roleAs) {
        List<Reservation> reservationList;
        // roleAs 값 검증은 따로 하지 않고 만약에 guardian, sitter 이외의 값이 들어오면 그냥 all list return
        if (roleAs == ReservationRole.GUARDIAN){
            reservationList = reservationRepository.findAllByGuardianId(memberId);
        }else if (roleAs == ReservationRole.SITTER){
            reservationList = reservationRepository.findAllBySitterMemberId(memberId);
        }else{
            reservationList = reservationRepository.findAllBySitterMemberIdOrGuardianId(memberId, memberId);
        }

//        List<Reservation> asGuardian = reservationRepository.findAllByGuardianId(memberId);
//        List<Reservation> asSitter = reservationRepository.findAllBySitterMemberId(memberId);
//
//        List<Reservation> all = new ArrayList<>(asGuardian);
//        all.addAll(asSitter);

        return reservationList.stream()
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
    @TrackExecutionTime("reservation.confirm")
    @Transactional
    @CacheEvict(cacheNames = "postings", allEntries = true, cacheManager = "shortTtlCacheManager")
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

                    // CONFIRMED 후 알림 발행
                    sendConfirmNotifications(reservation);
                    return toResponseDto(reservation);
                });
    }


    private void sendConfirmNotifications(Reservation reservation) {
        // 1. 보호자에게 결제 완료 알림
        notificationBroker.publish(NotificationEvent.of(
                reservation.getGuardianId(),
                null,
                SseEventType.PAYMENT_COMPLETED,
                "결제가 완료되어 예약이 확정되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        // 2. 시터에게 결제 완료 알림
        notificationBroker.publish(NotificationEvent.of(
                reservation.getSitterMemberId(),
                null,
                SseEventType.PAYMENT_COMPLETED,
                "결제가 완료되어 예약이 확정되었습니다.",
                reservation.getId(),
                "RESERVATION"
        ));

        // 3. Proposal 출처인 경우: 탈락한 시터들에게 WITHDRAWN 알림 (N명)
        if (reservation.getSource() == ReservationSource.PROPOSAL) {
            Proposal accepted = proposalRepository.findById(reservation.getSourceId())
                    .orElse(null);

            if (accepted != null) {
                proposalRepository.findAllByPostId(accepted.getPostId())
                        .stream()
                        .filter(p -> p.getStatus() == ProposalStatus.REJECTED
                                || p.getStatus() == ProposalStatus.WITHDRAWN)
                        .filter(p -> !p.getSitterMemberId().equals(reservation.getSitterMemberId()))
                        .forEach(p -> {
                            notificationBroker.publish(NotificationEvent.of(
                                    p.getSitterMemberId(),
                                    null,
                                    SseEventType.PROPOSAL_WITHDRAWN,
                                    "다른 시터가 선택되어 제안이 자동 철회되었습니다.",
                                    p.getId(),
                                    "PROPOSAL"
                            ));
                            log.info("{} => 제안 철회 알림: sitterMemberId={}, proposalId={}",
                                    NAME, p.getSitterMemberId(), p.getId());
                        });
            }
        }

        log.info("{} => 예약 확정 알림 발행: reservationId={}, guardianId={}, sitterMemberId={}",
                NAME, reservation.getId(), reservation.getGuardianId(), reservation.getSitterMemberId());
    }


    /*
    케어 완료 처리
    시터만 호출 가능, CONFIRMED 상태에서만 가능
     */
    @DistributedLock(key = "'reservation:' + #reservationId")
    @TrackExecutionTime("reservation.complete")
    @Transactional
    public ReservationResponseDto complete(Long memberId, Long reservationId) {
        /*
        시스템이 보관하고 있던 돈을 시터에게 보내주는 로직
        - complete 를 호출한 사람 (로그인한 사람) == reservation 의 sitter 인지 확인
        - completed 가 가능한 상태인지 (confirmed) 확인
        - 타임슬롯 마지막 시간까지 마쳤는지 확인
         */
        Reservation reservation = findById(reservationId);
        validateSitter(memberId, reservation);
        validateConfirmed(reservation);
        validateCareCompleted(reservationId);

        Payment guardianPayment = findPaidGuardianPayment(reservationId);

        //status update
        reservation.complete();
        settlementService.createCareCompletionSettlement(
                reservation.getId(),
                reservation.getSitterMemberId(),
                guardianPayment.getId(),
                guardianPayment.getFinalAmount()
        );
        paymentRefundService.refundSitterDepositAfterCompletion(reservationId);
        log.info("[케어 완료] reservationId={}, 시터(memberId={}) 완료 처리", reservationId, memberId);

        return toResponseDto(reservation);
    }

    /*
    예약 취소
    PENDING 또는 CONFIRMED 상태만 취소 가능
    예약 당사자만 취소 가능
    취소 사유 필수 (최소 10자)
     */
    @DistributedLock(key = "'reservation:' + #reservationId")
    @TrackExecutionTime("reservation.cancel")
    @Transactional
    public ReservationResponseDto cancel(Long memberId, Long reservationId, CancelReservationRequest request) {
        Reservation reservation = findById(reservationId);
        validateParty(memberId, reservation);
        validateCancelable(reservation);

        CanceledBy canceledBy = reservation.isGuardian(memberId) ? CanceledBy.GUARDIAN : CanceledBy.SITTER;

        // reservation 이 Pending 인 경우: 그냥.. 환불해주면 됨
        if (reservation.isPending()){
            reservation.cancel(request.cancelReason(), request.cancelCategory(), canceledBy);
            log.info("[예약 취소] reservationId={}, 취소 주체={}, 사유={}", reservationId, canceledBy, request.cancelReason());

            paymentRefundService.refundPaidPayments(reservationId, request.cancelReason());
            paymentRefundService.cancelNonPaidPayments(reservationId, request.cancelReason());

            // Proposal 출처인 경우: ACCEPTED → PENDING 복원, 공고 OPEN 유지
            handleCancellation(reservation);

            return toResponseDto(reservation);
        }

        //  reservation 이 Confirmed 가 된 경우
        //  : 어쩔 수 없는 일이 아니라면 무조건 위약금을 물게 됨

        // 어쩔수 없는 일인지? -> 관리자 검토 후 위약금 안 물게 해줄 수도 있고 아닐 수도 있어요...
        if (request.cancelCategory() == CancelCategory.UNAVOIDABLE) {
            reservation.requestCancel(request.cancelReason(), request.cancelCategory(), canceledBy);
            log.info("[취소 요청] UNAVOIDABLE 관리자 검토 대기 reservationId={}, 취소주체={}",
                    reservationId, canceledBy);
            return toResponseDto(reservation);
        }

        // 그냥 취소하는거면? -> 위약금 차감 후 취소
        reservation.cancel(request.cancelReason(), request.cancelCategory(), canceledBy);
        log.info("[예약 취소] CONFIRMED 위약금 차감 취소 reservationId={}, 취소주체={}, 사유={}",
                reservationId, canceledBy, request.cancelReason());

        paymentRefundService.refundWithPenalty(reservationId, request.cancelReason(), canceledBy);
        paymentRefundService.cancelNonPaidPayments(reservationId, request.cancelReason());
        handleCancellation(reservation);
        return toResponseDto(reservation);
    }

    // 예약 만료 처리는 ReservationExpireScheduler + ReservationExpireService 로 이동

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

    private Payment findPaidGuardianPayment(Long reservationId) {
        return paymentRepository.findByReservationIdAndPaymentRoleAndStatus(
                        reservationId, PaymentRole.GUARDIAN, PaymentStatus.PAID)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    // CONFIRMED 예약과 충돌하는지 확인하기 위해서 예약목록을 돌면서 체크
    public boolean hasConfirmedConflict(Long sitterProfileId, List<? extends HasTimeSlotInfo> timeSlots) {
        List<Reservation> confirmed = reservationRepository
                .findAllBySitterProfileIdAndStatus(sitterProfileId, ReservationStatus.CONFIRMED);

        for (Reservation existing : confirmed) {
            List<ReservationTimeSlot> existingSlots = reservationTimeSlotRepository
                    .findAllByReservationIdOrderByTimeSlotInfoSequence(existing.getId());

            if (timeSlotValidator.hasTimeConflict(existingSlots, timeSlots)) {
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

    // complete 가능한 상태인지 확인 == 현재 confirmed 인지 확인
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
        List<ReservationTimeSlot> newTimeSlots = reservationTimeSlotRepository
                .findAllByReservationIdOrderByTimeSlotInfoSequence(reservation.getId());

        if (hasConfirmedConflict(reservation.getSitterProfileId(), newTimeSlots)) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_CONFLICT);
        }
    }

    /*
    CONFIRMED 후속 처리
    - 충돌 PENDING 예약 Cancel 처리
    - Proposal 출처: 같은 공고의 나머지 PENDING 제안 → REJECTED, 공고 → CLOSED
    - 같은 시터의 겹치는 시간대 Proposal → WITHDRAWN
     */
    private void handlePostConfirmation(Reservation reservation) {
        cancelConflictingReservations(reservation);

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

    private void cancelConflictingReservations(Reservation confirmedReservation) {
        List<ReservationTimeSlot> confirmedSlots =
                reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(confirmedReservation.getId());

        List<Reservation> sitterPendingReservations = reservationRepository
                .findAllBySitterProfileIdAndStatus(confirmedReservation.getSitterProfileId(), ReservationStatus.PENDING)
                .stream()
                .filter(r -> !r.getId().equals(confirmedReservation.getId()))
                .toList();

        for (Reservation pending : sitterPendingReservations) {
            List<ReservationTimeSlot> slots = reservationTimeSlotRepository.findAllByReservationIdOrderByTimeSlotInfoSequence(pending.getId());
            if (!timeSlotValidator.hasTimeConflict(slots, confirmedSlots)) {
                continue;
            }
            pending.cancel("시터의 다른 예약 확정으로 인한 자동 취소", CancelCategory.SCHEDULE_CHANGE,CanceledBy.SITTER);

            refundIfPaid(pending);
        }
    }

    private void refundIfPaid(Reservation reservation) {
        Long reservationId = reservation.getId();
        ReservationPayment rp = reservationPaymentRepository.findByReservationId(reservationId)
                .orElse(null);
        // PENDING 예약이긴 한데 결제를 아무도 안 함
        if (rp == null) return;

        paymentRefundService.refundPaidPayments(reservationId, "시터의 다른 예약 확정으로 인한 자동 취소");

        // ReservationService.refundIfPaid
        // -> paymentRefundService.refundPaidPayments
        // -> paymentRefundService.refund
        // -> paymentRefundService.restoreReservationPayment 를 호출하는데 이 메서드에서 이미 ReservationPayment 의
        // XXXPaid 를 다시 Refund 하는 로직이 포함되어있음 == 아래는 중복 로직이므로 주석처리

//        if (rp.isSitterPaid()){
//            rp.sitterRefund();
//        }
//
//        if (rp.isGuardianPaid()){
//            rp.guardianRefund();
//        }
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
    - CareRequest 출처: ACCEPT -> PENDING 복원
     */
    private void handleCancellation(Reservation reservation) {
        if (reservation.getSource() == ReservationSource.PROPOSAL) {
            proposalRepository.findById(reservation.getSourceId())
                    .ifPresent(Proposal::restoreToPending);
            return;
        }
        careRequestRepository.findById(reservation.getSourceId())
                .ifPresent(CareRequest::restoreToPending);
    }

    /*
    timeslot 들을 가져와서 호출 시점이 마지막 날짜의 종료 시간 이후인지 확인
     */
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

}
