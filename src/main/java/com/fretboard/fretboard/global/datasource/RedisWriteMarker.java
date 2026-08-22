package com.fretboard.fretboard.global.datasource;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed {@link WriteMarker} that records a short-lived "recent write" marker per user
 * so subsequent read-only reads route to the primary datasource (read-your-writes).
 *
 * <p>Marker key format: {@code user:{userId}:recentWrite}.
 */
@Component
public class RedisWriteMarker implements WriteMarker {

    private static final Logger log = LoggerFactory.getLogger(RedisWriteMarker.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${read-write-split.recent-write-ttl-millis:3000}")
    private long recentWriteTtlMillis = 3000L;

    public RedisWriteMarker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void mark(Long userId) {
        String key = key(userId);
        try {
            redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(recentWriteTtlMillis));
        } catch (RuntimeException e) {
            // best-effort / fail-open: 마킹 실패가 쓰기 흐름을 막지 않는다.
            log.warn("Failed to mark recent write for key={}", key, e);
        }
    }

    @Override
    public boolean hasRecentWrite(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    private String key(Long userId) {
        return "user:" + userId + ":recentWrite";
    }
}
