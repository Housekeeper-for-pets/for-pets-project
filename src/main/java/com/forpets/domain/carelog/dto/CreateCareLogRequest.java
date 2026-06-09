package com.forpets.domain.carelog.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateCareLogRequest(
        @NotBlank(message = "일지 내용은 필수입니다.")
        String content,
        List<String> imageUrls
) {}