package com.forpets.domain.carerequest.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CareRequestErrorCode implements ErrorCode {

    CARE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 돌봄 요청입니다."),
    SITTER_NOT_RESERVABLE(HttpStatus.BAD_REQUEST, "C002", "시터가 예약 불가능 상태입니다."),
    CANNOT_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST, "C003", "본인의 프로필에 돌봄 요청할 수 없습니다."),
    NOT_CARE_REQUEST_OWNER(HttpStatus.FORBIDDEN, "C003", "본인이 생성한 돌봄 요청이 아닙니다."),
    NOT_TARGET_SITTER(HttpStatus.FORBIDDEN, "C003", "본인이 받은 돌봄 요청이 아닙니다."),
    NOT_PENDING_CARE_REQUEST(HttpStatus.BAD_REQUEST, "C003", "해당 요청은 돌봄 요청이 대기상태 일 때 가능합니다."),
    DUPLICATE_PENDING_REQUEST(HttpStatus.BAD_REQUEST, "C003", "중복된 돎봄 요청입니다."),
    NOT_CARE_REQUEST_PARTY(HttpStatus.BAD_REQUEST, "C003", "돌봄 요청 상세 조회는 요청 본인 또는 시터 당사자만 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
