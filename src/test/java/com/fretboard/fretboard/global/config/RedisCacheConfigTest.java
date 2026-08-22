package com.fretboard.fretboard.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.fretboard.fretboard.global.common.CacheKey;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisCacheConfigTest {

    private RedisCacheConfig redisCacheConfig;

    @BeforeEach
    void setUp() {
        redisCacheConfig = new RedisCacheConfig();
    }

    @Test
    void 캐시별_TTL이_개별_적용된다() {
        // given
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        // when
        RedisCacheManager cacheManager =
                (RedisCacheManager) redisCacheConfig.postCacheManager(connectionFactory);
        Map<String, RedisCacheConfiguration> configs = cacheManager.getCacheConfigurations();

        // then — 캐시별로 서로 다른 TTL이 개별 지정되어야 한다
        assertThat(ttlOf(configs, CacheKey.RECENT_POSTS))
                .isEqualTo(Duration.ofMinutes(CacheKey.RECENT_POSTS_TTL));
        assertThat(ttlOf(configs, CacheKey.BOARDS))
                .isEqualTo(Duration.ofMinutes(CacheKey.BOARDS_TTL_MINUTES));
        assertThat(ttlOf(configs, CacheKey.BOARD))
                .isEqualTo(Duration.ofMinutes(CacheKey.BOARDS_TTL_MINUTES));
        assertThat(ttlOf(configs, CacheKey.POST_DETAIL))
                .isEqualTo(Duration.ofMinutes(CacheKey.POST_DETAIL_TTL_MINUTES));
        assertThat(ttlOf(configs, CacheKey.COMMENTS))
                .isEqualTo(Duration.ofMinutes(CacheKey.COMMENTS_TTL_MINUTES));
        assertThat(ttlOf(configs, CacheKey.POPULAR_TODAY))
                .isEqualTo(Duration.ofSeconds(CacheKey.POPULAR_TTL_SECONDS));
        assertThat(ttlOf(configs, CacheKey.POPULAR_BY_BOARD))
                .isEqualTo(Duration.ofSeconds(CacheKey.POPULAR_TTL_SECONDS));

        // 인기글(짧음)은 게시판(김)보다 짧아야 한다
        assertThat(ttlOf(configs, CacheKey.POPULAR_TODAY))
                .isLessThan(ttlOf(configs, CacheKey.BOARDS));
    }

    @Test
    void Redis장애_모의시_예외없이_DB_fallback한다() {
        // given
        CacheErrorHandler errorHandler = redisCacheConfig.errorHandler();
        assertThat(errorHandler).isNotNull();

        org.springframework.cache.Cache cache = mock(org.springframework.cache.Cache.class);
        RuntimeException redisDown = new RuntimeException("redis down");

        // when & then — get/put/evict/clear 실패를 삼키고 예외를 전파하지 않아야 한다
        assertThatCode(() -> errorHandler.handleCacheGetError(redisDown, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCachePutError(redisDown, cache, "key", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheEvictError(redisDown, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheClearError(redisDown, cache))
                .doesNotThrowAnyException();
    }

    private Duration ttlOf(Map<String, RedisCacheConfiguration> configs, String cacheName) {
        RedisCacheConfiguration config = configs.get(cacheName);
        assertThat(config)
                .as("캐시 '%s' 에 대한 개별 설정이 존재해야 한다", cacheName)
                .isNotNull();
        return config.getTtlFunction().getTimeToLive(Object.class, null);
    }
}
