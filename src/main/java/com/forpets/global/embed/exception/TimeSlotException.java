package com.forpets.global.embed.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class TimeSlotException extends BusinessException {
    public TimeSlotException(ErrorCode errorCode) {
        super(errorCode);
    }

    public TimeSlotException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
