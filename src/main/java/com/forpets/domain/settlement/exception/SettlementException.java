package com.forpets.domain.settlement.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class SettlementException extends BusinessException {
    public SettlementException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SettlementException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
