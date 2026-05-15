package com.forpets.domain.member.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원과 인증 도메인에서 사용하는 에러 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "이미 가입된 이메일입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다."),

    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "이메일 또는 비밀번호가 일치하지 않습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "정지된 계정입니다."),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "ACCOUNT_DELETED", "탈퇴한 계정입니다."),

    HAS_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "HAS_ACTIVE_RESERVATION", "진행 중인 예약이 있어 탈퇴할 수 없습니다."),

    INVALID_GENDER(HttpStatus.BAD_REQUEST, "INVALID_GENDER", "허용되지 않은 성별 값입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "SAME_PASSWORD", "새 비밀번호가 기존 비밀번호와 동일합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}