package com.forpets.global.embed.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum TimeSlotErrorCode implements ErrorCode {

    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "시작 시간은 종료 시간보다 빨라야합니다."),
    TIME_SLOT_REQUIRED(HttpStatus.BAD_REQUEST, "TIME_SLOT_REQUIRED", "TimeSlot 은 최소 하나 등록이 필요합니다."),
    TIMESLOT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "TIMESLOT_LIMIT_EXCEEDED", "TimeSlot 은 공고, 요청 당 30개만 가능합니다."),
    PAST_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "PAST_DATE_NOT_ALLOWED", "과거 날짜를 추가할 수 없습니다."),
    DUPLICATE_TIME_SLOT(HttpStatus.BAD_REQUEST, "DUPLICATE_TIME_SLOT", "TimeSlot 이 중복됩니다."),

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
