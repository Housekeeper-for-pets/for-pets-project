package com.forpets.domain.sitter.dto.profile;

import java.util.List;

/**
 * 시터 목록 검색 결과 응답 DTO입니다.
 * API 명세의 페이지네이션 응답 구조(content, totalElements, totalPages, currentPage, size)를 따릅니다.
 */
public record SitterPageResponse(
        List<SitterResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static SitterPageResponse of(
            List<SitterResponseDto> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new SitterPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
