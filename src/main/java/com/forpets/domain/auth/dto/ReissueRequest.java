package com.forpets.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token으로 Access Token을 재발급하기 위한 요청 DTO입니다.
 */
public record ReissueRequest(

        @NotBlank(message = "Refresh Token은 필수입니다.")
        String refreshToken
) {
}