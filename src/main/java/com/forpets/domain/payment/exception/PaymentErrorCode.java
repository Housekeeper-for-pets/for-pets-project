package com.forpets.domain.payment.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "존재하지 않는 결제입니다."),
    NOT_PAYMENT_PARTY(HttpStatus.FORBIDDEN, "NOT_PAYMENT_PARTY", "결제 요청 권한이 없습니다."),
    DUPLICATE_PAYMENT_REQUEST(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT_REQUEST", "이미 진행 중인 결제가 있습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_STATUS", "결제 가능한 상태가 아닙니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
