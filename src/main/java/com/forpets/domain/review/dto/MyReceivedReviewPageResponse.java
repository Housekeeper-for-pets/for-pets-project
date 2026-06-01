package com.forpets.domain.review.dto;

import java.util.List;

public record MyReceivedReviewPageResponse(
        List<MyReceivedReviewResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static MyReceivedReviewPageResponse of(
            List<MyReceivedReviewResponse> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new MyReceivedReviewPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
