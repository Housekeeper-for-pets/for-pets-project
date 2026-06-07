package com.forpets.domain.ai.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record RagSearchRequest(
        @NotBlank(message = "검색 문장은 필수입니다")
        String query
) {
}
