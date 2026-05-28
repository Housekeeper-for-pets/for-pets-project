package com.forpets.domain.carelog.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CareLogErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."),

    NOT_SITTER_OF_RESERVATION(HttpStatus.FORBIDDEN, "NOT_SITTER_OF_RESERVATION", "해당 예약의 시터만 일지를 작성할 수 있습니다."),
    NOT_RESERVATION_PARTICIPANT(HttpStatus.FORBIDDEN, "NOT_RESERVATION_PARTICIPANT", "해당 예약의 당사자만 일지를 조회할 수 있습니다."),

    INVALID_RESERVATION_STATUS(HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_STATUS", "케어 중(CONFIRMED) 상태에서만 일지를 등록할 수 있습니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;

}
