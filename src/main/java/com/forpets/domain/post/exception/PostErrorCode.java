package com.forpets.domain.post.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 공고입니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "P002", "본인의 공고가 아닙니다."),
    POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "P003", "종료된 공고입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "P004", "수정 불가한 공고 상태입니다.")


    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
