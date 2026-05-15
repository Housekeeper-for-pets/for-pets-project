package com.forpets.global.exception;

import lombok.Getter;

/**
 * 서비스 계층에서 의도적으로 발생시키는 비즈니스 예외의 부모 클래스입니다.
 * ErrorCode를 함께 들고 있어서 GlobalExceptionHandler가 일관된 응답으로 변환할 수 있습니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}