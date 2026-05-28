package com.forpets.domain.carelog.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class CareLogException extends BusinessException {
    public CareLogException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CareLogException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
