package com.forpets.global.config;

import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/*
ShedLock 설정 — 멀티 인스턴스 환경에서 스케줄러 중복 실행 방지

역할 분리:
 - 엔티티 락 (lock:reservation:{id} 등)   : 비즈니스 로직 동시성 보호
 - 스케줄러 락 (shedlock:{name})           : 배치 주기 자체의 단일성 보장 (이 설정)

defaultLockAtMostFor = PT10M
 - 인스턴스가 처리 중 죽었을 때 최대 10분 후 다음 인스턴스가 인계받을 수 있도록 함
 - 개별 스케줄러에서 @SchedulerLock 의 lockAtMostFor 로 override 가능

키 prefix = "for-pets:shedlock"
 - 기존 엔티티 락 (lock:*) 과 키 공간 분리
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory, "for-pets:shedlock");
    }
}
