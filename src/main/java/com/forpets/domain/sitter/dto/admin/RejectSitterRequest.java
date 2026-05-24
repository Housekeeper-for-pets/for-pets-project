package com.forpets.domain.sitter.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSitterRequest(
        @NotBlank(message = "거절 사유는 필수입니다")
        @Size(min = 10, message = "거절 사유는 최소 10자 이상이어야 합니다")
        String rejectReason
) {}