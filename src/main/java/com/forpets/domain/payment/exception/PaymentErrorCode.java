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
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다."),
    PAYMENT_ID_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_ID_MISMATCH", "결제 ID가 일치하지 않습니다."),
    PORTONE_PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST, "PORTONE_PAYMENT_NOT_PAID", "결제가 완료되지 않았습니다."),
    PORTONE_PAYMENT_VERIFY_FAILED(HttpStatus.BAD_REQUEST, "PORTONE_PAYMENT_VERIFY_FAILED", "PortOne 결제 검증에 실패했습니다."),
    PORTONE_PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "PORTONE_PAYMENT_CANCEL_FAILED", "PortOne 결제 취소에 실패했습니다."),
    PAYMENT_LOCK_FAILED(HttpStatus.CONFLICT, "PAYMENT_LOCK_FAILED", "결제 처리 중입니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
