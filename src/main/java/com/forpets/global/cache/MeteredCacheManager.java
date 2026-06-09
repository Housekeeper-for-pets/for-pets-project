package com.forpets.global.cache;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link CacheManager} 데코레이터: getCache로 반환되는 Cache를 {@link MeteredCache}로 감싸 hit/miss를 계측한다.
 *
 * <p>매니저 레벨에서 감싸므로 현재 캐시(sitters/sitter/postings)뿐 아니라 이후 추가되는 캐시도 자동 계측된다.
 * 래핑한 Cache는 이름별로 캐싱하여 Counter가 중복 등록되지 않도록 한다.
 */
public class MeteredCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Cache> wrappedCaches = new ConcurrentHashMap<>();

    public MeteredCacheManager(CacheManager delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        return wrappedCaches.computeIfAbsent(name, ignored -> new MeteredCache(cache, meterRegistry));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}
