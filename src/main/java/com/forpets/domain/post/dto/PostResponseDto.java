package com.forpets.domain.post.dto;

import com.forpets.global.embed.dto.PetSnapshotResponseDto;
import com.forpets.global.embed.dto.TimeSlotResponseDto;
import com.forpets.domain.post.entity.Post;
import com.forpets.domain.post.entity.PostPet;
import com.forpets.domain.post.entity.PostStatus;
import com.forpets.domain.post.entity.PostTimeSlot;
import com.forpets.global.common.CareType;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponseDto(
        Long id,
        Long memberId,
        String title,
        String content,
        String region,
        CareType careType,
        Integer budgetAmount,
        PostStatus status,
        List<PetSnapshotResponseDto> pets,
        List<TimeSlotResponseDto> timeSlots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /*
    상태 기반 스냅샷 노출 정책:
    - OPEN: pets, timeSlots 전체 노출
    - CLOSED, DELETED: pets, timeSlots 빈 리스트로 반환
     */
    public static PostResponseDto from(Post post, List<PostPet> pets, List<PostTimeSlot> timeSlots) {
        boolean showDetails = post.isOpen();

        return new PostResponseDto(
                post.getId(),
                post.getMemberId(),
                post.getTitle(),
                post.getContent(),
                post.getRegion(),
                post.getCareType(),
                post.getBudgetAmount(),
                post.getStatus(),
                showDetails
                        ? pets.stream().map(PetSnapshotResponseDto::from).toList()
                        : List.of(),
                showDetails
                        ? timeSlots.stream().map(TimeSlotResponseDto::from).toList()
                        : List.of(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}