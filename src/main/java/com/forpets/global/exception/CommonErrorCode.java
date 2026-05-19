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
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "요청 파라미터 값이 올바르지 않습니다."),

    // ----------- PET ------------

//    INVALID_PET_SPECIES(HttpStatus.BAD_REQUEST, "PET001", "허용되지 않은 반려동물 종류입니다."),
//    INVALID_PET_SIZE(HttpStatus.BAD_REQUEST, "PET002", "허용되지 않은 반려동물 크기입니다."),
//    PET_CORE_FIELD_CHANGE_RESTRICTED(HttpStatus.BAD_REQUEST, "PET003", "예약이 진행중이라 주요 정보 수정이 불가능한 상태입니다."),
//    INVALID_PET_GENDER(HttpStatus.BAD_REQUEST, "PET004", "허용되지 않은 반려동물 성별입니다."),
//    PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PET005", "최대 10마리까지만 등록 가능합니다"),
//    NOT_PET_OWNER(HttpStatus.FORBIDDEN, "PET006", "본인의 반려동물이 아닙니다."),
//    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "PET007", "존재하지 않는 반려동물입니다."),
//    PET_USED_IN_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "PET008", "진행 중인 예약에 포함된 반려동물은 수정 및 삭제가 불가합니다."),
//    PET_USED_IN_OPEN_POST(HttpStatus.BAD_REQUEST, "PET009", "열려있는 공고에 포함된 반려동물은 삭제가 불가능합니다."),
//    PET_USED_IN_PENDING_REQUEST(HttpStatus.BAD_REQUEST, "PET010", "수락 대기중인 요청에 포함된 반려동물은 삭제가 불가능합니다."),

    // ------------- SITTER -------------

    ADMIN_CANNOT_REGISTER_SITTER(HttpStatus.FORBIDDEN, "S001", "관리자는 시터 프로필을 등록할 수 없습니다."),
    SITTER_PROFILE_EXISTS(HttpStatus.BAD_REQUEST, "S002", "이미 시터 프로필이 존재합니다."),
    SITTER_PROFILE_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "S003", "시터 프로필을 삭제 한 이후 다시 생성할 수 없습니다."),
    SITTER_NOT_FOUND(HttpStatus.NOT_FOUND, "S004", "존재하지 않는 시터입니다."),

    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "S005", "시작 시간은 종료 시간보다 빨라야합니다."),
    DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "S006", "요일별 1개 시간대만 등록 가능합니다."),
    NOT_SCHEDULE_OWNER(HttpStatus.FORBIDDEN, "S007", "본인의 스케줄이 아닙니다."),
    HAS_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "S008", "CONFIRMED 상태의 예약이 있습니다."),

    // ------------ TIME SLOT ----------------

    TIME_SLOT_REQUIRED(HttpStatus.BAD_REQUEST, "T001", "TimeSlot 은 최소 하나 등록이 필요합니다."),
    TIMESLOT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T002", "TimeSlot 은 공고, 요청 당 30개만 가능합니다."),
    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T003", "과거 날짜를 추가할 수 없습니다."),
    DUPLICATE_TIME_SLOT(HttpStatus.BAD_REQUEST, "T004", "TimeSlot 이 중복됩니다."),

    // -------- POST & PROPOSAL ---------
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 공고입니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "P002", "본인의 공고가 아닙니다."),
    POST_NOT_OPEN(HttpStatus.BAD_REQUEST, "P003", "종료된 공고입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "P004", "수정 불가한 공고 상태입니다."),

    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "존재하지 않는 제안입니다."),
    CANNOT_PROPOSE_OWN_POST(HttpStatus.BAD_REQUEST, "P006", "본인의 공고에 지원할 수 없습니다."),
    DUPLICATE_PROPOSAL(HttpStatus.BAD_REQUEST, "P007", "중복된 제안입니다."),
    NOT_PENDING_PROPOSAL(HttpStatus.BAD_REQUEST, "P008", "해당 요청은 제안이 대기상태 일 때 가능합니다."),
    NOT_PROPOSAL_PARTY(HttpStatus.FORBIDDEN, "P009", "제안 상세 조회는 공고 작성자 또는 제안한 시터만 가능합니다."),
    HAS_ACTIVE_PROPOSAL(HttpStatus.BAD_REQUEST, "P010", "대기 또는 채택된 제안이 존재합니다."),

    // -------- CARE REQUEST -----

//    CARE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않는 돌봄 요청입니다."),
//    SITTER_NOT_RESERVABLE(HttpStatus.BAD_REQUEST, "C002", "시터가 예약 불가능 상태입니다."),
//    CANNOT_REQUEST_TO_SELF(HttpStatus.BAD_REQUEST, "C003", "본인의 프로필에 돌봄 요청할 수 없습니다."),
//    NOT_CARE_REQUEST_OWNER(HttpStatus.FORBIDDEN, "C003", "본인이 생성한 돌봄 요청이 아닙니다."),
//    NOT_TARGET_SITTER(HttpStatus.FORBIDDEN, "C003", "본인이 받은 돌봄 요청이 아닙니다."),
//    NOT_PENDING_CARE_REQUEST(HttpStatus.BAD_REQUEST, "C003", "해당 요청은 돌봄 요청이 대기상태 일 때 가능합니다."),
//    DUPLICATE_PENDING_REQUEST(HttpStatus.BAD_REQUEST, "C003", "중복된 돎봄 요청입니다."),
//    NOT_CARE_REQUEST_PARTY(HttpStatus.BAD_REQUEST, "C003", "돌봄 요청 상세 조회는 요청 본인 또는 시터 당사자만 가능합니다."),

    // ---- RERSERVATION ---------
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "존재하지 않는 예약입니다."),
    RESERVATION_CONFLICT(HttpStatus.CONFLICT, "R002", "해당 시간에 이미 확정된 예약이 있습니다."),
    NOT_RESERVATION_PARTY(HttpStatus.FORBIDDEN, "R003", "예약 조회는 당사자만 가능합니다."),
    INVALID_RESERVATION_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "R004", "변경 가능한 예약 상태가 아닙니다."),
    NOT_RESERVATION_SITTER(HttpStatus.FORBIDDEN, "R005", "해당 예약의 시터가 아닙니다."),
    CARE_NOT_COMPLETED_YET(HttpStatus.BAD_REQUEST, "R006", "아직 돌봄이 끝나지 않았습니다."),
    ALREADY_PAID(HttpStatus.BAD_REQUEST, "R007", "중복 결제입니다."),



    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}