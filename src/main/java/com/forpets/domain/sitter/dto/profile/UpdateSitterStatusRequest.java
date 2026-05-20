package com.forpets.domain.sitter.dto.profile;

import com.forpets.domain.sitter.entity.SitterProfileStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSitterStatusRequest(
        @NotNull(message = "시터 상태는 필수입니다")
        SitterProfileStatus status
) {}
