package com.forpets.domain.member.exception;

import com.forpets.global.exception.BusinessException;
import com.forpets.global.exception.ErrorCode;

/**
 * 회원 도메인에서 발생하는 비즈니스 예외입니다.
 */
public class MemberException extends BusinessException {

    public MemberException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MemberException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}