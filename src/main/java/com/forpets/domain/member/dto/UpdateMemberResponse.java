package com.forpets.domain.member.dto;

import com.forpets.domain.member.entity.Member;
import com.forpets.domain.member.entity.MemberGender;

import java.time.LocalDateTime;

public record UpdateMemberResponse(
        Long id,
        String nickname,
        String phone,
        MemberGender gender,
        LocalDateTime updatedAt
) {

    public static UpdateMemberResponse from(Member member) {
        return new UpdateMemberResponse(
                member.getId(),
                member.getNickname(),
                member.getPhone(),
                member.getGender(),
                member.getUpdatedAt()
        );
    }
}
