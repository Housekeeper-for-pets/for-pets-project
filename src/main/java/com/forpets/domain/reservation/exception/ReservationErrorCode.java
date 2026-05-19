package com.forpets.domain.reservation.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "존재하지 않는 예약입니다."),
    RESERVATION_CONFLICT(HttpStatus.CONFLICT, "R002", "해당 시간에 이미 확정된 예약이 있습니다."),
    NOT_RESERVATION_PARTY(HttpStatus.FORBIDDEN, "R003", "예약 조회는 당사자만 가능합니다."),
    INVALID_RESERVATION_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "R004", "변경 가능한 예약 상태가 아닙니다."),
    NOT_RESERVATION_SITTER(HttpStatus.FORBIDDEN, "R005", "해당 예약의 시터가 아닙니다."),
    CARE_NOT_COMPLETED_YET(HttpStatus.BAD_REQUEST, "R006", "아직 돌봄이 끝나지 않았습니다."),
    ALREADY_PAID(HttpStatus.BAD_REQUEST, "R007", "중복 결제입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
