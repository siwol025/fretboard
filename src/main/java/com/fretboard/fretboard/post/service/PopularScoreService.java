package com.fretboard.fretboard.post.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularScoreService {

    private static final String KEY_PREFIX = "popular:today:";
    private static final Duration TTL = Duration.ofDays(2);

    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 3.0;
    private static final double COMMENT_WEIGHT = 2.0;

    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;

    public void applyView(Long postId) {
        addScore(postId, VIEW_WEIGHT);
    }

    public void applyLike(Long postId) {
        addScore(postId, LIKE_WEIGHT);
    }

    public void cancelLike(Long postId) {
        addScore(postId, -LIKE_WEIGHT);
    }

    public void applyComment(Long postId) {
        addScore(postId, COMMENT_WEIGHT);
    }

    public void cancelComment(Long postId) {
        addScore(postId, -COMMENT_WEIGHT);
    }

    public List<Long> getTopPostIds(int limit) {
        try {
            Set<Object> ranked = redisTemplate.opsForZSet().reverseRange(todayKey(), 0, limit - 1);
            if (ranked == null || ranked.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> postIds = new ArrayList<>(ranked.size());
            for (Object element : ranked) {
                postIds.add(Long.parseLong(element.toString()));
            }
            return postIds;
        } catch (Exception e) {
            log.warn("Failed to read popular post ids from Redis", e);
            return Collections.emptyList();
        }
    }

    private void addScore(Long postId, double delta) {
        try {
            String key = todayKey();
            redisTemplate.opsForZSet().incrementScore(key, postId, delta);
            redisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.warn("Failed to update popular score in Redis (postId={}, delta={})", postId, delta, e);
        }
    }

    private String todayKey() {
        return KEY_PREFIX + LocalDate.now(clock);
    }
}
