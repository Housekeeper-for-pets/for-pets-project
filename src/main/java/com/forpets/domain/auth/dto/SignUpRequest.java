package com.forpets.domain.auth.dto;

import com.forpets.domain.member.entity.MemberGender;
import com.forpets.domain.member.entity.Region;
import jakarta.validation.constraints.*;

/**
 * 회원가입 요청 DTO입니다.
 * 역할은 요청으로 받지 않고 서버에서 MEMBER로 고정합니다.
 */
public record SignUpRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 최대 100자까지 입력할 수 있습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 50, message = "닉네임은 최대 50자까지 입력할 수 있습니다.")
        String nickname,

        @Size(max = 30, message = "전화번호는 최대 30자까지 입력할 수 있습니다.")
        String phone,

        MemberGender gender,
        Region region
) {
}