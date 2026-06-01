package com.forpets.domain.review.dto;

import java.util.List;

public record MyWrittenReviewPageResponse(
        List<MyWrittenReviewResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static MyWrittenReviewPageResponse of(
            List<MyWrittenReviewResponse> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new MyWrittenReviewPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
