package com.forpets.domain.carerequest.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CareRequestErrorCode implements ErrorCode {

    CARE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CARE_REQUEST_NOT_FOUND", "존재하지 않는 돌봄 요청입니다."),
    SITTER_NOT_RESERVABLE(HttpStatus.BAD_REQUEST, "SITTER_NOT_RESERVABLE", "시터가 예약 불가능 상태입니다."),
    CANNOT_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST, "CANNOT_REQUEST_TO_SELF", "본인의 프로필에 돌봄 요청할 수 없습니다."),
    NOT_CARE_REQUEST_OWNER(HttpStatus.FORBIDDEN, "NOT_CARE_REQUEST_OWNER", "본인이 생성한 돌봄 요청이 아닙니다."),
    NOT_TARGET_SITTER(HttpStatus.FORBIDDEN, "NOT_TARGET_SITTER", "본인이 받은 돌봄 요청이 아닙니다."),
    NOT_PENDING_CARE_REQUEST(HttpStatus.BAD_REQUEST, "NOT_PENDING_CARE_REQUEST", "해당 요청은 돌봄 요청이 대기상태 일 때 가능합니다."),
    DUPLICATE_PENDING_REQUEST(HttpStatus.CONFLICT, "DUPLICATE_PENDING_REQUEST", "중복된 돎봄 요청입니다."),
    NOT_CARE_REQUEST_PARTY(HttpStatus.FORBIDDEN, "NOT_CARE_REQUEST_PARTY", "돌봄 요청 상세 조회는 요청 본인 또는 시터 당사자만 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
