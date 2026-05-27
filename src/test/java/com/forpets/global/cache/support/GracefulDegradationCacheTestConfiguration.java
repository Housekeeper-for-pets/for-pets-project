package com.forpets.global.cache.support;

import com.forpets.global.cache.GracefulDegradationCacheErrorHandler;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Graceful Degradation 통합 테스트용 캐시 설정.
 * Redis 없이 실패하는 CacheManager + CacheErrorHandler를 주입합니다.
 */
@Configuration
@EnableCaching
public class GracefulDegradationCacheTestConfiguration implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new GracefulDegradationCacheErrorHandler();
    }

    @Bean("longTtlCacheManager")
    @Primary
    public CacheManager longTtlCacheManager() {
        return new FailingCacheManager("sitters", "sitter");
    }

    @Bean("shortTtlCacheManager")
    public CacheManager shortTtlCacheManager() {
        return new FailingCacheManager("postings", "adminPendingSitters");
    }
}
