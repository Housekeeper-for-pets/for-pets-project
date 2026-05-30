package com.forpets.domain.review.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND", "예약을 찾을 수 없습니다."),
    RESERVATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "RESERVATION_NOT_COMPLETED", "완료된 예약에만 리뷰를 작성할 수 있습니다."),
    NOT_RESERVATION_GUARDIAN(HttpStatus.FORBIDDEN, "NOT_RESERVATION_GUARDIAN", "해당 예약의 보호자만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 리뷰가 존재합니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "INVALID_RATING", "평점은 1점 이상 5점 이하로 입력해주세요."),
    INVALID_REVIEW_COMMENT(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_COMMENT", "리뷰 내용은 필수이며 10자 이상 500자 이하로 입력해주세요."),
    CONTAIN_BAD_WORD(HttpStatus.BAD_REQUEST, "CONTAIN_BAD_WORD", "리뷰 내용에 금칙어가 포함되어 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
