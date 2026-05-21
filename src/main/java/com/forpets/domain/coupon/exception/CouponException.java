package com.forpets.domain.coupon.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

public class CouponException extends BusinessException {

    public CouponException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CouponException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
