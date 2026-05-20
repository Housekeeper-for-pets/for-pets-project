package com.forpets.domain.post.dto;

import com.forpets.domain.member.entity.Region;
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
        Region region,
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
    public static PostResponseDto from(Post post, Region region, List<PostPet> pets, List<PostTimeSlot> timeSlots) {
        return new PostResponseDto(
                post.getId(),
                post.getMemberId(),
                post.getTitle(),
                post.getContent(),
                region,
                post.getCareType(),
                post.getBudgetAmount(),
                post.getStatus(),
                pets.stream().map(p->PetSnapshotResponseDto.of(p.getPetId(), p.getPetSnapshot())).toList(),
                timeSlots.stream().map(t->TimeSlotResponseDto.of(t.getId(), t.getTimeSlotInfo())).toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}