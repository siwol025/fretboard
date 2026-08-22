package com.fretboard.fretboard.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fretboard.fretboard.global.common.CacheKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("캐시 조회 실패, DB fallback: cache={}, key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("캐시 저장 실패: cache={}, key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("캐시 제거 실패: cache={}, key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("캐시 clear 실패: cache={}", cacheName(cache), exception);
            }

            private String cacheName(Cache cache) {
                return cache != null ? cache.getName() : "unknown";
            }
        };
    }

    @Bean
    public CacheManager postCacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(CacheKey.RECENT_POSTS_TTL));

        Duration boardsTtl = Duration.ofMinutes(CacheKey.BOARDS_TTL_MINUTES);
        Duration popularTtl = Duration.ofSeconds(CacheKey.POPULAR_TTL_SECONDS);

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheKey.RECENT_POSTS,
                baseConfig.entryTtl(Duration.ofMinutes(CacheKey.RECENT_POSTS_TTL)));
        cacheConfigurations.put(CacheKey.BOARDS, baseConfig.entryTtl(boardsTtl));
        cacheConfigurations.put(CacheKey.BOARD, baseConfig.entryTtl(boardsTtl));
        cacheConfigurations.put(CacheKey.POST_DETAIL,
                baseConfig.entryTtl(Duration.ofMinutes(CacheKey.POST_DETAIL_TTL_MINUTES)));
        cacheConfigurations.put(CacheKey.COMMENTS,
                baseConfig.entryTtl(Duration.ofMinutes(CacheKey.COMMENTS_TTL_MINUTES)));
        cacheConfigurations.put(CacheKey.POPULAR_TODAY, baseConfig.entryTtl(popularTtl));
        cacheConfigurations.put(CacheKey.POPULAR_BY_BOARD, baseConfig.entryTtl(popularTtl));

        RedisCacheManager cacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
        cacheManager.initializeCaches();
        return cacheManager;
    }
}
