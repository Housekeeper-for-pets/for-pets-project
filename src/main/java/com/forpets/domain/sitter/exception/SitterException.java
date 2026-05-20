package com.forpets.domain.sitter.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class SitterException extends BusinessException {
    public SitterException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SitterException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
