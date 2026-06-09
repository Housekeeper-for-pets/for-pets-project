package com.forpets.global.cache.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 지정한 cache name에 대해 {@link FailingCache}를 제공하는 테스트용 CacheManager.
 */
public class FailingCacheManager implements CacheManager {

    private final Map<String, Cache> caches;

    public FailingCacheManager(String... cacheNames) {
        this.caches = Arrays.stream(cacheNames)
                .collect(Collectors.toMap(name -> name, FailingCache::new));
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, FailingCache::new);
    }

    @Override
    public Collection<String> getCacheNames() {
        return caches.keySet();
    }
}
