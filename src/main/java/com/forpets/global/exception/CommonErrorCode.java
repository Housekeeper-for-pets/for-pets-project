package com.forpets.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인과 관계없이 공통으로 사용할 에러 코드입니다.
 * 세부 도메인 에러는 각 도메인 패키지에서 별도로 확장합니다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "요청 데이터가 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "INVALID_ENUM_VALUE", "허용되지 않은 값입니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST", "페이지 요청 값이 올바르지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "만료된 Refresh Token입니다."),

    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),

    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 처리되었거나 충돌이 발생한 요청입니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // ----------- PET ------------

    INVALID_PET_SPECIES(HttpStatus.BAD_REQUEST, "P002", "허용되지 않은 반려동물 종류입니다."),
    INVALID_PET_SIZE(HttpStatus.BAD_REQUEST, "P003", "허용되지 않은 반려동물 크기입니다."),
    INVALID_PET_GENDER(HttpStatus.BAD_REQUEST, "P004", "허용되지 않은 반려동물 성별입니다."),
    PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P005", "최대 10마리까지만 등록 가능합니다"),
    NOT_PET_OWNER(HttpStatus.FORBIDDEN, "P006", "본인의 반려동물이 아닙니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P007", "존재하지 않는 반려동물입니다."),
    PET_USED_IN_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "P008", "진행 중인 예약에 포함된 반려동물은 수정 및 삭제가 불가합니다."),
    PET_USED_IN_OPEN_POST(HttpStatus.BAD_REQUEST, "P009", "열려있는 공고에 포함된 반려동물은 삭제가 불가능합니다."),
    PET_USED_IN_PENDING_REQUEST(HttpStatus.BAD_REQUEST, "P010", "수락 대기중인 요청에 포함된 반려동물은 삭제가 불가능합니다."),
    PET_CORE_FIELD_CHANGE_RESTRICTED(HttpStatus.BAD_REQUEST, "P011", "예약이 진행중이라 주요 정보 수정이 불가능한 상태입니다.")

    // ---------------------------------



    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}