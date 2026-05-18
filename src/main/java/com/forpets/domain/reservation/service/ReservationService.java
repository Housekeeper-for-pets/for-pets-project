package com.forpets.domain.reservation.service;

import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.dto.ReservationResponseDto;
import com.forpets.domain.reservation.entity.Reservation;
import com.forpets.domain.reservation.entity.ReservationPayment;
import com.forpets.domain.reservation.entity.ReservationPet;
import com.forpets.domain.reservation.entity.ReservationTimeSlot;
import com.forpets.domain.reservation.repository.ReservationPaymentRepository;
import com.forpets.domain.reservation.repository.ReservationPetRepository;
import com.forpets.domain.reservation.repository.ReservationRepository;
import com.forpets.domain.reservation.repository.ReservationTimeSlotRepository;
import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationPetRepository reservationPetRepository;
    private final ReservationTimeSlotRepository reservationTimeSlotRepository;
    private final ReservationPaymentRepository reservationPaymentRepository;


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

    // transaction 아닌 애들

    public Reservation findById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESERVATION_NOT_FOUND));
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
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESERVATION_NOT_FOUND));
    }
}
