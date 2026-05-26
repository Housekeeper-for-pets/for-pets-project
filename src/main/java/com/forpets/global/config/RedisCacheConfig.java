package com.forpets.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forpets.domain.post.dto.PostPageResponse;
import com.forpets.domain.sitter.dto.profile.SitterPageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@EnableCaching
@Configuration
public class RedisCacheConfig implements CachingConfigurer {//errorHandler 오버라이드 위함

    // 캐시 만료 시간(TTL) 설정을 위해 시간의 양을 표현하는 Duration 클래스 활용
    private static final Duration LONG_TTL = Duration.ofHours(1);
    private static final Duration SHORT_TTL = Duration.ofMinutes(5);
    private static final Duration EMPTY_RESULT_TTL = Duration.ofMinutes(1);

    @Bean
    @Primary
    public RedisCacheManager longTtlCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration(objectMapper, this::longTtl))
                .build();
    }

    @Bean
    public RedisCacheManager shortTtlCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration(objectMapper, this::shortTtl))
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache get failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache put failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache evict failed. cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache clear failed. cache={}", cache.getName(), exception);
            }
        };
    }

    private RedisCacheConfiguration redisCacheConfiguration(
            ObjectMapper objectMapper,
            RedisCacheWriter.TtlFunction ttlFunction
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer(objectMapper)))
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":")
                .entryTtl(ttlFunction)
                .disableCachingNullValues();
    }

    private GenericJackson2JsonRedisSerializer redisSerializer(ObjectMapper objectMapper) {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(redisObjectMapper, null);
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    private Duration longTtl(Object key, Object value) {
        log.info("TTL 결정 — value type: {}, value: {}",
                value == null ? "null" : value.getClass().getName(), value);
        if (isEmptySearchResult(value)) {
            log.info("빈 결과 감지 → 1분 TTL 적용");
            return EMPTY_RESULT_TTL;
        }
        log.info("일반 결과 → 1시간 TTL 적용");
        return LONG_TTL;
    }

    private Duration shortTtl(Object key, Object value) {
        if (isEmptySearchResult(value)) {
            return EMPTY_RESULT_TTL;
        }
        return SHORT_TTL;
    }

    private boolean isEmptySearchResult(Object value) {
        if (value instanceof SitterPageResponse response) {
            return response.totalElements() == 0;
        }
        if (value instanceof PostPageResponse response) {
            return response.totalElements() == 0;
        }
        return false;
    }
}
