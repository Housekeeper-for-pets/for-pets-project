package com.forpets.domain.notification.exception;

import com.forpets.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOT_NOTIFICATION_RECEIVER(HttpStatus.FORBIDDEN, "NOT_NOTIFICATION_RECEIVER", "본인 알림만 읽음 처리 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}