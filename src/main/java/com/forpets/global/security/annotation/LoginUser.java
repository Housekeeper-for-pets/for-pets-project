package com.forpets.global.security.annotation;

import java.lang.annotation.*;

/**
 * 컨트롤러 메서드 파라미터에서 로그인 회원 정보를 바로 주입받기 위한 어노테이션입니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}