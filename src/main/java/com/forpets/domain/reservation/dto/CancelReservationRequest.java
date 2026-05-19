package com.forpets.domain.reservation.dto;

import com.forpets.domain.reservation.entity.CancelCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        @NotBlank(message = "취소 사유는 필수입니다")
        @Size(min = 10, message = "취소 사유는 최소 10자 이상이어야 합니다")
        String cancelReason,

        @NotNull(message = "취소 분류는 필수입니다")
        CancelCategory cancelCategory
) {}
