package com.forpets.global.cache;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GracefulDegradationCacheErrorHandlerTest {

    private final GracefulDegradationCacheErrorHandler handler = new GracefulDegradationCacheErrorHandler();

    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(GracefulDegradationCacheErrorHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("GET 장애 시 예외를 전파하지 않고 [CACHE_DEGRADED] 로그를 남긴다")
    void handleCacheGetError_does_not_propagate() {
        Cache cache = mock(Cache.class);
        given(cache.getName()).willReturn("sitters");
        RuntimeException exception = new RedisConnectionFailureException("connection refused");

        assertThatCode(() -> handler.handleCacheGetError(exception, cache, "test-key"))
                .doesNotThrowAnyException();

        assertThat(logAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("[CACHE_DEGRADED]")
                        && message.contains("operation=GET")
                        && message.contains("cache=sitters"));
    }

    @Test
    @DisplayName("PUT 장애 시 예외를 전파하지 않는다")
    void handleCachePutError_does_not_propagate() {
        Cache cache = mock(Cache.class);
        given(cache.getName()).willReturn("postings");

        assertThatCode(() -> handler.handleCachePutError(
                new RedisConnectionFailureException("down"), cache, "key", "value"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("EVICT 장애 시 예외를 전파하지 않는다")
    void handleCacheEvictError_does_not_propagate() {
        Cache cache = mock(Cache.class);
        given(cache.getName()).willReturn("sitters");

        assertThatCode(() -> handler.handleCacheEvictError(
                new RedisConnectionFailureException("down"), cache, "key"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CLEAR 장애 시 예외를 전파하지 않는다")
    void handleCacheClearError_does_not_propagate() {
        Cache cache = mock(Cache.class);
        given(cache.getName()).willReturn("sitters");

        assertThatCode(() -> handler.handleCacheClearError(
                new RedisConnectionFailureException("down"), cache))
                .doesNotThrowAnyException();
    }

}
