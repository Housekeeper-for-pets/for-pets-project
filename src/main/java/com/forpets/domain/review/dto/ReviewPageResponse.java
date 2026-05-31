package com.forpets.domain.review.dto;

import java.util.List;

public record ReviewPageResponse(
        List<ReviewResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static ReviewPageResponse of(
            List<ReviewResponse> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new ReviewPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
