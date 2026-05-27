package com.forpets.global.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Redis(Spring Cache) 장애 시 예외를 전파하지 않고 비즈니스 로직을 계속 수행합니다.
 * <ul>
 *   <li>GET 실패 → cache miss로 처리되어 DB 조회로 폴백</li>
 *   <li>PUT/EVICT/CLEAR 실패 → 쓰기 API는 정상 완료 (캐시만 스킵)</li>
 * </ul>
 * 정책: 캐싱정책 v1.3 §11-14 Redis 장애 대응
 */
@Slf4j
public class GracefulDegradationCacheErrorHandler implements CacheErrorHandler {

    private static final String DEGRADED_PREFIX = "[CACHE_DEGRADED]";

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("{} operation=GET cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("{} operation=PUT cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("{} operation=EVICT cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("{} operation=CLEAR cache={}", DEGRADED_PREFIX, cache.getName(), exception);
    }
}
