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

    //Grafana나 로그 수집 도구에서 [CACHE_DEGRADED] 키워드 하나로 모든 캐시 장애 로그를 필터링할 수 있게 상수처리
    private static final String DEGRADED_PREFIX = "[CACHE_DEGRADED]";

    //@Cacheable 메서드에서 Redis 조회 실패 시 호출
    //Spring이 cache miss로 간주하고 메서드 본문(DB 조회)을 실행
    //느려질수 있지만 정상응답
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("{} operation=GET cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }


    //DB 조회 후 결과를 Redis에 저장할 때 실패하면 호출
    //호출시 캐시 저장만 건너뛰고 DB 조회 결과를 그대로 반환
    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("{} operation=PUT cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }

    //@CacheEvict로 캐시를 삭제할 때 실패하면 호출
    //예외를 삼키면 캐시 삭제만 실패하고 approve(), create() 같은 비즈니스 로직은 정상 완료
    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("{} operation=EVICT cache={} key={}", DEGRADED_PREFIX, cache.getName(), key, exception);
    }

    //allEntries = true로 전체 캐시를 지울 때 실패하면 호출
    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("{} operation=CLEAR cache={}", DEGRADED_PREFIX, cache.getName(), exception);
    }
}
