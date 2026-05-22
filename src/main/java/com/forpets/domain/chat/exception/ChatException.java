package com.forpets.domain.chat.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class ChatException extends BusinessException {

    public ChatException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ChatException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
