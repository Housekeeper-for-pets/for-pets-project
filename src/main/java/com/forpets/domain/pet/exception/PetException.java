package com.forpets.domain.pet.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class PetException extends BusinessException {

    public PetException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PetException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
