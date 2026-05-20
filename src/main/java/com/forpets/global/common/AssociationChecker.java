package com.forpets.global.common;

import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.repository.CareRequestRepository;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.repository.PostRepository;
import com.forpets.domain.proposal.entity.ProposalStatus;
import com.forpets.domain.proposal.repository.ProposalRepository;
import com.forpets.domain.reservation.entity.ReservationStatus;
import com.forpets.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssociationChecker {

    private final ReservationRepository reservationRepository;
    private final PostRepository postRepository;
    private final CareRequestRepository careRequestRepository;
    private final ProposalRepository proposalRepository;

    // Pet 삭제 시
    public boolean hasPetActiveAssociation(Long petId) {
        return existsActiveReservationByPetId(petId)
                || existsActivePostByPetId(petId)
                || existsActiveCareRequestByPetId(petId);
    }

    // Sitter 탈퇴 시
    public boolean hasSitterActiveAssociation(Long sitterId) {
        return existsActiveReservationBySitterId(sitterId)
                || existsActiveProposalBySitterId(sitterId)
                || existsActiveCareRequestBySitterId(sitterId);
    }

    private boolean existsActiveReservationByPetId(Long petId) {
        return reservationRepository.existsByPetIdAndStatusIn(
                petId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    }

    private boolean existsActivePostByPetId(Long petId) {
        return postRepository.existsByPetIdAndStatusIn(
                petId, List.of(PostStatus.OPEN));
    }

    private boolean existsActiveCareRequestByPetId(Long petId) {
        return careRequestRepository.existsByPetIdAndStatusIn(
                petId, List.of(CareRequestStatus.PENDING, CareRequestStatus.ACCEPTED));
    }

    private boolean existsActiveReservationBySitterId(Long sitterId) {
        return reservationRepository.existsBySitterIdAndStatusIn(
                sitterId, List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED));
    }

    private boolean existsActiveProposalBySitterId(Long sitterId) {
        return proposalRepository.existsBySitterProfileIdAndStatusIn(
                sitterId, List.of(ProposalStatus.PENDING));
    }

    private boolean existsActiveCareRequestBySitterId(Long sitterId) {
        return careRequestRepository.existsBySitterProfileIdAndStatusIn(
                sitterId, List.of(CareRequestStatus.PENDING, CareRequestStatus.ACCEPTED));
    }
}