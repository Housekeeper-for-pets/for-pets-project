package com.forpets.domain.coupon.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements ErrorCode {

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "존재하지 않는 쿠폰입니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "COUPON_ALREADY_USED", "이미 사용된 쿠폰입니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "COUPON_NOT_OWNED", "해당 유저의 쿠폰이 아닙니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "COUPON_ALREADY_ISSUED", "이미 발급받은 쿠폰입니다."),
    COUPON_QUANTITY_EXHAUSTED(HttpStatus.BAD_REQUEST, "COUPON_QUANTITY_EXHAUSTED", "잔여 쿠폰 수량이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
