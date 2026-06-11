package com.forpets.domain.post.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공고 도메인 전용 에러 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    // 조회 / 권한
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "존재하지 않는 공고입니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "NOT_POST_AUTHOR", "본인의 공고가 아닙니다."),

    // 상태
    POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "POST_NOT_OPEN", "종료된 공고입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "INVALID_STATUS_TRANSITION", "수정 불가한 공고 상태입니다."),
    INVALID_POST_STATUS(HttpStatus.BAD_REQUEST, "INVALID_POST_STATUS", "허용되지 않은 공고 상태입니다."),

    // 검색 / 필터
    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_CONDITION", "검색 조건이 올바르지 않습니다."),
    INVALID_CARE_TYPE(HttpStatus.BAD_REQUEST, "INVALID_CARE_TYPE", "허용되지 않은 돌봄 유형입니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", "페이지 번호 또는 크기 값이 올바르지 않습니다."),
    INVALID_SORT_FIELD(HttpStatus.BAD_REQUEST, "INVALID_SORT_FIELD", "허용되지 않은 정렬 필드입니다."),

    // 락 — 만료 스케줄러와 다른 작업(수정/닫기/삭제) 사이의 동시성 보호
    POST_LOCK_FAILED(HttpStatus.TOO_MANY_REQUESTS, "POST_LOCK_FAILED", "공고 처리 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
