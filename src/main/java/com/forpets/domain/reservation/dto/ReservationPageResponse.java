package com.forpets.domain.reservation.dto;

import java.util.List;

/**
 * 예약 목록 페이지 응답 DTO
 * 기존 PostPageResponse / SitterPageResponse 와 동일한 구조 유지
 */
public record ReservationPageResponse(
        List<ReservationResponseDto> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size
) {
    public static ReservationPageResponse of(
            List<ReservationResponseDto> content,
            long totalElements,
            int totalPages,
            int currentPage,
            int size
    ) {
        return new ReservationPageResponse(content, totalElements, totalPages, currentPage, size);
    }
}
