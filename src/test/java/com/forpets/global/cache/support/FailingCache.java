package com.forpets.global.cache.support;

import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.concurrent.Callable;

/**
 * 테스트용 Cache — 모든 연산에서 Redis 장애를 시뮬레이션합니다.
 */
public class FailingCache implements Cache {

    private final String name;

    public FailingCache(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        throw connectionFailure("GET");
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        throw connectionFailure("GET");
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        throw connectionFailure("GET");
    }

    @Override
    public void put(Object key, Object value) {
        throw connectionFailure("PUT");
    }

    @Override
    public void evict(Object key) {
        throw connectionFailure("EVICT");
    }

    @Override
    public void clear() {
        throw connectionFailure("CLEAR");
    }

    private RedisConnectionFailureException connectionFailure(String operation) {
        return new RedisConnectionFailureException(
                "Simulated Redis failure on " + operation + " for cache=" + name);
    }
}
