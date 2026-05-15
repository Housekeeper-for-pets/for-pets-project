package com.forpets.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드를 공통 규격으로 관리하기 위한 인터페이스입니다.
 * 공통 에러와 도메인별 에러가 같은 방식으로 처리되게 합니다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}