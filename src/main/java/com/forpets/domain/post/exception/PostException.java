package com.forpets.domain.post.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class PostException extends BusinessException {
    public PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PostException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
