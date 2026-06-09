package com.forpets.global.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ExecutionTimeAspect {

    private final MeterRegistry meterRegistry;

    /*
     * @TrackExecutionTime이 붙은 메서드의 실행 시간 측정
     * 성공/실패 여부도 tag로 함께 기록 -> 실패 케이스만 따로 분석 가능
     */
    @Around("@annotation(com.forpets.global.monitoring.TrackExecutionTime)")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        TrackExecutionTime trackExecutionTime = signature.getMethod().getAnnotation(TrackExecutionTime.class);

        Timer.Sample sample = Timer.start(meterRegistry);
        String metricName = trackExecutionTime.value();
        String methodName = joinPoint.getSignature().getName();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = "failure";
            throw e;
        } finally {
            sample.stop(Timer.builder(metricName + ".duration")
                    .description(metricName + " 메서드 실행 시간")
                    .tag("method", methodName)
                    .tag("status", status)
                    .register(meterRegistry));
        }
    }
}