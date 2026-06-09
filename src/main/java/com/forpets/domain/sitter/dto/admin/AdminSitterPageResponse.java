package com.forpets.domain.sitter.dto.admin;

import java.util.List;

/**
 * 관리자 시터 승인대기 목록 페이지 응답 DTO
 * 기존 SitterPageResponse / PostPageResponse 와 동일한 구조 유지
 */
public record AdminSitterPageResponse(
        List<AdminSitterResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static AdminSitterPageResponse of(
            List<AdminSitterResponseDto> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new AdminSitterPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
