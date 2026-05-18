package com.forpets.domain.sitter.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 시터 도메인 전용 에러 코드입니다.
 * API 명세의 시터 목록 검색 에러 코드를 포함합니다.
 */
@Getter
@RequiredArgsConstructor
public enum SitterErrorCode implements ErrorCode {

    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_CONDITION", "검색 조건이 올바르지 않습니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", "페이지 번호 또는 크기 값이 올바르지 않습니다."),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, "INVALID_SORT_FIELD", "허용되지 않은 정렬 필드입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
