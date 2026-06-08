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
    NOT_RESERVATION_PARTY(HttpStatus.FORBIDDEN, "NOT_RESERVATION_PARTY", "해당 예약의 당사자가 아닙니다."),
    INVALID_RESERVATION_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_STATUS_TRANSITION", "변경 가능한 예약 상태가 아닙니다."),
    NOT_RESERVATION_SITTER(HttpStatus.FORBIDDEN, "NOT_RESERVATION_SITTER", "해당 예약의 시터가 아닙니다."),
    CARE_NOT_COMPLETED_YET(HttpStatus.BAD_REQUEST, "CARE_NOT_COMPLETED_YET", "아직 돌봄이 끝나지 않았습니다."),
    ALREADY_PAID(HttpStatus.CONFLICT, "ALREADY_PAID", "중복 결제입니다."),
    RESERVATION_CONFIRM_LOCK_FAILED(HttpStatus.TOO_MANY_REQUESTS, "RESERVATION_CONFIRM_LOCK_FAILED", "예약 확정 처리 중입니다. 잠시 후 다시 시도해주세요."),
    RESERVATION_LOCK_FAILED(HttpStatus.TOO_MANY_REQUESTS, "RESERVATION_LOCK_FAILED", "예약 처리 중입니다. 잠시 후 다시 시도해주세요."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", "페이지 번호 또는 크기 값이 올바르지 않습니다."),
    CARE_LOG_EXISTS_CANNOT_CANCEL(HttpStatus.CONFLICT, "CARE_LOG_EXISTS_CANNOT_CANCEL", "케어 일지가 등록된 예약은 취소할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
