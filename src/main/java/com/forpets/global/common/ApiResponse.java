package com.forpets.global.common;

import com.forpets.global.exception.ErrorResponse;

/**
 * 모든 API 응답을 success, data, error 구조로 통일합니다.
 * 성공 응답은 data만 채우고, 실패 응답은 error만 채웁니다.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> successEmpty() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}