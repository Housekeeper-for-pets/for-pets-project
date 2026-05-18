package com.forpets.domain.carerequest.dto;

import com.forpets.domain.carerequest.entity.CareRequest;
import com.forpets.domain.carerequest.entity.CareRequestPet;
import com.forpets.domain.carerequest.entity.CareRequestStatus;
import com.forpets.domain.carerequest.entity.CareRequestTimeSlot;
import com.forpets.global.common.CareType;
import com.forpets.global.embed.dto.PetSnapshotResponseDto;
import com.forpets.global.embed.dto.TimeSlotResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public record CareRequestResponseDto(
        Long id,
        Long memberId,
        Long sitterProfileId,
        CareType careType,
        String message,
        int requestPrice,
        CareRequestStatus status,
        List<PetSnapshotResponseDto> pets,
        List<TimeSlotResponseDto> timeSlots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CareRequestResponseDto from(CareRequest request,
                                              List<CareRequestPet> pets,
                                              List<CareRequestTimeSlot> timeSlots) {
        return new CareRequestResponseDto(
                request.getId(),
                request.getMemberId(),
                request.getSitterProfileId(),
                request.getCareType(),
                request.getMessage(),
                request.getRequestPrice(),
                request.getStatus(),
                pets.stream().map(p-> PetSnapshotResponseDto.of(p.getPetId(), p.getPetSnapshot())).toList(),
                timeSlots.stream().map(t-> TimeSlotResponseDto.of(t.getId(), t.getTimeSlotInfo())).toList(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}