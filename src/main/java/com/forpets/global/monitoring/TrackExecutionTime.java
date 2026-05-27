package com.forpets.global.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 메서드 실행 시간을 측정 -> Prometheus 메트릭으로 볼 수 있게 하는 어노테이션
 *   @TrackExecutionTime("reservation.confirm")
 *      public void confirmAfterPayment 이렇게 씀
 *      -> payment_confirm_duration_seconds 메트릭이 Prometheus에 사용됨
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackExecutionTime {
    /*
     * 메트릭 이름이 reservation.confirm 이라면
     * Prometheus에선 reservation_confirm 으로 접근하기
     */
    String value();
}