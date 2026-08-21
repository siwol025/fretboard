package com.fretboard.fretboard.post.service;

import java.time.Clock;
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

    private final RedisTemplate<String, Object> redisTemplate;
    private final Clock clock;
    private final com.fretboard.fretboard.global.config.PopularProperties popularProperties;

    public void applyView(Long postId) {
        addScore(postId, popularProperties.weights().view());
    }

    public void applyLike(Long postId) {
        addScore(postId, popularProperties.weights().like());
    }

    public void cancelLike(Long postId) {
        addScore(postId, -popularProperties.weights().like());
    }

    public void applyComment(Long postId) {
        addScore(postId, popularProperties.weights().comment());
    }

    public void cancelComment(Long postId) {
        addScore(postId, -popularProperties.weights().comment());
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
            redisTemplate.expire(key, popularProperties.todayTtl());
        } catch (Exception e) {
            log.warn("Failed to update popular score in Redis (postId={}, delta={})", postId, delta, e);
        }
    }

    private String todayKey() {
        return KEY_PREFIX + LocalDate.now(clock);
    }
}
