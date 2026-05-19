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
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, "INVALID_SORT_FIELD", "허용되지 않은 정렬 필드입니다."),

    ADMIN_CANNOT_REGISTER_SITTER(HttpStatus.FORBIDDEN, "ADMIN_CANNOT_REGISTER_SITTER", "관리자는 시터 프로필을 등록할 수 없습니다."),
    SITTER_PROFILE_EXISTS(HttpStatus.BAD_REQUEST, "SITTER_PROFILE_EXISTS", "이미 시터 프로필이 존재합니다."),
    SITTER_PROFILE_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "SITTER_PROFILE_ALREADY_REGISTERED", "시터 프로필을 삭제 한 이후 다시 생성할 수 없습니다."),
    SITTER_NOT_FOUND(HttpStatus.NOT_FOUND, "SITTER_NOT_FOUND", "존재하지 않는 시터입니다."),

    DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "DUPLICATE_SCHEDULE", "요일별 1개 시간대만 등록 가능합니다."),
    NOT_SCHEDULE_OWNER(HttpStatus.FORBIDDEN, "NOT_SCHEDULE_OWNER", "본인의 스케줄이 아닙니다."),
    HAS_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "HAS_ACTIVE_RESERVATION", "CONFIRMED 상태의 예약이 있습니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
