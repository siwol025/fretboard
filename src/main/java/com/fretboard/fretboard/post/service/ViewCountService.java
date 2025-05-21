package com.fretboard.fretboard.post.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VIEW_COUNT_KEY = "post:viewCount";

    public boolean hasViewCount(Long postId) {
        return redisTemplate.opsForHash().hasKey(VIEW_COUNT_KEY, postId.toString());
    }

    public void setInitialViewCount(Long postId, Long count) {
        redisTemplate.opsForHash().put(VIEW_COUNT_KEY, postId.toString(), count);
    }

    public Long increaseViewCount(Long postId) {
        return redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, postId.toString(), 1);
    }

    public Map<Object, Object> getAllViewCounts() {
        return redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
    }

    public void deleteViewCount(Long postId) {
        if (hasViewCount(postId)) {
            redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, postId.toString());
        }
    }

    public void clearAllViewCounts() {
        redisTemplate.delete(VIEW_COUNT_KEY);
    }
}
