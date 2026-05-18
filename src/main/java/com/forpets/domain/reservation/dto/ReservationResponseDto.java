package com.forpets.domain.reservation.dto;

import com.forpets.domain.reservation.entity.*;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.dto.PetSnapshotResponseDto;
import com.forpets.global.embed.dto.TimeSlotResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public record ReservationResponseDto(
        Long id,
        Long guardianId,
        Long sitterMemberId,
        Long sitterProfileId,
        CareType careType,
        ReservationStatus status,
        ReservationSource source,
        boolean guardianPaid,
        boolean sitterPaid,
        String cancelReason,
        CancelCategory cancelCategory,
        CanceledBy canceledBy,
        List<PetSnapshotResponseDto> pets,
        List<TimeSlotResponseDto> timeSlots,
        LocalDateTime confirmedAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime expiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReservationResponseDto from(Reservation reservation,
                                              ReservationPayment payment,
                                              List<ReservationPet> pets,
                                              List<ReservationTimeSlot> timeSlots) {
        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getGuardianId(),
                reservation.getSitterMemberId(),
                reservation.getSitterProfileId(),
                reservation.getCareType(),
                reservation.getStatus(),
                reservation.getSource(),
                payment.isGuardianPaid(),
                payment.isSitterPaid(),
                reservation.getCancelReason(),
                reservation.getCancelCategory(),
                reservation.getCanceledBy(),
                pets.stream()
                        .map(rp -> PetSnapshotResponseDto.of(rp.getPetId(), rp.getPetSnapshot()))
                        .toList(),
                timeSlots.stream()
                        .map(ts -> TimeSlotResponseDto.of(ts.getId(), ts.getTimeSlotInfo()))
                        .toList(),
                reservation.getConfirmedAt(),
                reservation.getCompletedAt(),
                reservation.getCanceledAt(),
                reservation.getExpiredAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}