package com.forpets.domain.reservation.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "존재하지 않는 예약입니다."),
    RESERVATION_CONFLICT(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", "해당 시간에 이미 확정된 예약이 있습니다."),
    NOT_RESERVATION_PARTY(HttpStatus.FORBIDDEN, "NOT_RESERVATION_PARTY", "예약 조회는 당사자만 가능합니다."),
    INVALID_RESERVATION_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_STATUS_TRANSITION", "변경 가능한 예약 상태가 아닙니다."),
    NOT_RESERVATION_SITTER(HttpStatus.FORBIDDEN, "NOT_RESERVATION_SITTER", "해당 예약의 시터가 아닙니다."),
    CARE_NOT_COMPLETED_YET(HttpStatus.BAD_REQUEST, "CARE_NOT_COMPLETED_YET", "아직 돌봄이 끝나지 않았습니다."),
    ALREADY_PAID(HttpStatus.BAD_REQUEST, "ALREADY_PAID", "중복 결제입니다."),
    RESERVATION_CONFIRM_LOCK_FAILED(HttpStatus.CONFLICT, "RESERVATION_CONFIRM_LOCK_FAILED", "예약 확정 처리 중입니다. 잠시 후 다시 시도해주세요."),
    RESERVATION_LOCK_FAILED(HttpStatus.CONFLICT, "RESERVATION_LOCK_FAILED", "예약 처리 중입니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
