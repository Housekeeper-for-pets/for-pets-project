package com.forpets.domain.ai.rag.dto;

import java.util.List;

public record RagSearchResponse(
        List<RagSearchResultDto> results
) {
}
