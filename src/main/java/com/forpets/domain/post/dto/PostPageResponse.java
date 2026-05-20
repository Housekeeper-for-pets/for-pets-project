package com.forpets.domain.post.dto;

import java.util.List;

/**
 * 공고 목록 검색 결과 응답 DTO입니다.
 */
public record PostPageResponse(
        List<PostResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static PostPageResponse of(
            List<PostResponseDto> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new PostPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
