package com.forpets.domain.notification.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class NotificationException extends BusinessException {
    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
