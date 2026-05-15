package com.forpets.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 실패 응답의 error 영역에 들어가는 공통 응답 DTO입니다.
 * API 명세의 status, code, message를 유지하면서 디버깅에 필요한 path, timestamp를 함께 제공합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {

    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getStatus().name(),
                errorCode.getCode(),
                errorCode.getMessage(),
                path
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                errorCode.getStatus().value(),
                errorCode.getStatus().name(),
                errorCode.getCode(),
                message,
                path
        );
    }
}