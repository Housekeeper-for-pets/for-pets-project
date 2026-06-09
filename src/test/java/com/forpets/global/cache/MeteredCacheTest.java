package com.forpets.global.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class MeteredCacheTest {

    private double count(MeterRegistry registry, String cacheName, String result) {
        return registry.get("cache.gets")
                .tag("cache", cacheName)
                .tag("result", result)
                .counter().count();
    }

    @Test
    @DisplayName("[성공] miss 후 hit이 각각 cache.gets 카운터로 집계된다")
    void miss_then_hit_are_counted() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MeteredCache cache = new MeteredCache(new ConcurrentMapCache("sitters"), registry);

        assertThat(cache.get("k1")).isNull();                  // miss
        cache.put("k1", "v1");
        assertThat(cache.get("k1").get()).isEqualTo("v1");     // hit

        assertThat(count(registry, "sitters", "miss")).isEqualTo(1.0);
        assertThat(count(registry, "sitters", "hit")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("[성공] MeteredCacheManager는 캐시를 MeteredCache로 감싸 hit/miss를 계측한다")
    void manager_wraps_cache_with_metering() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MeteredCacheManager manager =
                new MeteredCacheManager(new ConcurrentMapCacheManager("postings"), registry);

        Cache cache = manager.getCache("postings");
        assertThat(cache).isInstanceOf(MeteredCache.class);

        cache.get("p1");          // miss
        cache.put("p1", "v");
        cache.get("p1");          // hit

        assertThat(count(registry, "postings", "miss")).isEqualTo(1.0);
        assertThat(count(registry, "postings", "hit")).isEqualTo(1.0);
        // 같은 이름은 같은 래퍼 인스턴스를 재사용(카운터 중복 등록 방지)
        assertThat(manager.getCache("postings")).isSameAs(cache);
    }
}
