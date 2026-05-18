package com.forpets.domain.member.dto;

import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 최대 50자까지 입력할 수 있습니다.")
        String nickname,

        @Size(max = 30, message = "전화번호는 최대 30자까지 입력할 수 있습니다.")
        String phone,

        MemberGender gender,

        Region region
) {
}
