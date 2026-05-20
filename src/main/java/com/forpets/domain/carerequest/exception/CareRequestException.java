package com.forpets.domain.carerequest.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class CareRequestException extends BusinessException {

    public CareRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CareRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}