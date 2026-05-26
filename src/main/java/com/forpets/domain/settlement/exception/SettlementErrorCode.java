package com.forpets.domain.settlement.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT_NOT_FOUND", "존재하지 않는 정산입니다."),
    NOT_SETTLEMENT_RECEIVER(HttpStatus.FORBIDDEN, "NOT_SETTLEMENT_RECEIVER", "정산 조회 권한이 없습니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "SETTLEMENT_ALREADY_EXISTS", "이미 생성된 정산이 있습니다."),
    INVALID_SETTLEMENT_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_AMOUNT", "정산 금액이 올바르지 않습니다."),
    INVALID_SETTLEMENT_TYPE(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_TYPE", "정산 유형이 올바르지 않습니다."),
    INVALID_SETTLEMENT_STATUS(HttpStatus.BAD_REQUEST, "INVALID_SETTLEMENT_STATUS", "정산 상태 전이가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
