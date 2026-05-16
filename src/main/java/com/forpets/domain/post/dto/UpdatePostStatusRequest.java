package com.forpets.domain.post.dto;

import com.forpets.domain.post.entity.PostStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePostStatusRequest(
        @NotNull(message = "공고 상태는 필수입니다")
        PostStatus status
) {}
