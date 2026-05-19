package com.forpets.domain.reservation.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class ReservationException extends BusinessException {
    public ReservationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ReservationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
