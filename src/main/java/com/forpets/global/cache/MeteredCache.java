package com.forpets.global.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Spring {@link Cache} 데코레이터: 조회(get) 시 hit/miss를 Micrometer Counter로 집계한다.
 *
 * <p>왜 여기서 계측하나:
 * <ul>
 *   <li>{@code @Cacheable}은 miss일 때만 메서드 본문이 실행되어, 서비스 코드에서는 hit을 셀 수 없다.</li>
 *   <li>{@code RedisCacheManager}/{@code RedisCache}는 hit/miss 통계를 제공하지 않아 Micrometer 자동계측 대상이 아니다.</li>
 * </ul>
 * 따라서 hit/miss가 모두 보이는 유일한 지점인 Cache 추상화의 {@link #get(Object)}에서 계측한다.
 *
 * <p>메트릭: {@code cache.gets{cache=<name>, result=hit|miss}} (Micrometer 표준 캐시 메트릭 규약과 동일)
 *
 * <p>현재 {@code @Cacheable}은 모두 sync=false라 인터셉터가 {@link #get(Object)}만 호출한다.
 * 중복 집계를 막기 위해 다른 get 오버로드는 계측하지 않고 그대로 위임한다.
 */
public class MeteredCache implements Cache {

    private final Cache delegate;
    private final Counter hitCounter;
    private final Counter missCounter;

    public MeteredCache(Cache delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.hitCounter = Counter.builder("cache.gets")
                .tag("cache", delegate.getName())
                .tag("result", "hit")
                .description("캐시 조회 결과 hit 건수")
                .register(meterRegistry);
        this.missCounter = Counter.builder("cache.gets")
                .tag("cache", delegate.getName())
                .tag("result", "miss")
                .description("캐시 조회 결과 miss 건수")
                .register(meterRegistry);
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = delegate.get(key);
        if (value != null) {
            hitCounter.increment();
        } else {
            missCounter.increment();
        }
        return value;
    }

    // ── 이하 위임만 (계측 안 함) ─────────────────────────────────────────
    @Override
    public <T> T get(Object key, Class<T> type) {
        return delegate.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return delegate.evictIfPresent(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean invalidate() {
        return delegate.invalidate();
    }
}
