package com.forpets.domain.review.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class ReviewException extends BusinessException {

    public ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReviewException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
