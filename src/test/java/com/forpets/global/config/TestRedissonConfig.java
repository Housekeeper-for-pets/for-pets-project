package com.forpets.global.config;

import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.TimeUnit;

@Configuration
@Profile("test")
public class TestRedissonConfig {

    @Bean
    public RedissonClient redissonClient() throws InterruptedException {
        RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
        RLock lock = Mockito.mock(RLock.class);

        Mockito.when(redissonClient.getLock(Mockito.anyString()))
                .thenReturn(lock);

        Mockito.when(lock.tryLock(
                        Mockito.anyLong(),
                        Mockito.anyLong(),
                        Mockito.any(TimeUnit.class)
                ))
                .thenReturn(true);

        Mockito.when(lock.isHeldByCurrentThread())
                .thenReturn(true);

        return redissonClient;
    }
}
