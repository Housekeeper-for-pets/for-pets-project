package com.forpets.domain.pet.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {

    INVALID_PET_SPECIES(HttpStatus.BAD_REQUEST, "INVALID_PET_SPECIES", "허용되지 않은 반려동물 종류입니다."),
    INVALID_PET_SIZE(HttpStatus.BAD_REQUEST, "INVALID_PET_SIZE", "허용되지 않은 반려동물 크기입니다."),
    PET_CORE_FIELD_CHANGE_RESTRICTED(HttpStatus.BAD_REQUEST, "PET_CORE_FIELD_CHANGE_RESTRICTED", "예약이 진행중이라 주요 정보 수정이 불가능한 상태입니다."),
    INVALID_PET_GENDER(HttpStatus.BAD_REQUEST, "INVALID_PET_GENDER", "허용되지 않은 반려동물 성별입니다."),
    PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PET_LIMIT_EXCEEDED", "최대 10마리까지만 등록 가능합니다"),
    NOT_PET_OWNER(HttpStatus.FORBIDDEN, "NOT_PET_OWNER", "본인의 반려동물이 아닙니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "PET_NOT_FOUND", "존재하지 않는 반려동물입니다."),
//    PET_USED_IN_ACTIVE_RESERVATION(HttpStatus.BAD_REQUEST, "PET_USED_IN_ACTIVE_RESERVATION", "진행 중인 예약에 포함된 반려동물은 수정 및 삭제가 불가합니다."),
//    PET_USED_IN_OPEN_POST(HttpStatus.BAD_REQUEST, "PET_USED_IN_OPEN_POST", "열려있는 공고에 포함된 반려동물은 삭제가 불가능합니다."),
    PET_USED_IN_ACTIVE_PROCESS(HttpStatus.BAD_REQUEST, "PET_USED_IN_ACTIVE_PROCESS", "활성상태의 공고, 예약 또는 돌봄 요청에 포함된 반려동물은 삭제가 불가능합니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
