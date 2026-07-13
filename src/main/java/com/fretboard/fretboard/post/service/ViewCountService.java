package com.fretboard.fretboard.post.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String VIEW_COUNT_KEY = "post:viewCount";
    private static final String POPULAR_KEY = "popular:posts";

    public boolean hasViewCount(Long postId) {
        return redisTemplate.opsForHash().hasKey(VIEW_COUNT_KEY, postId.toString());
    }

    public void setInitialViewCount(Long postId, Long count) {
        redisTemplate.opsForHash().put(VIEW_COUNT_KEY, postId.toString(), count);
    }

    public Long incrementViewCount(Long postId) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        zSetOps.incrementScore(POPULAR_KEY, postId, 1);
        redisTemplate.expire(POPULAR_KEY, Duration.ofHours(24));
        return redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, postId.toString(), 1);
    }

    public List<Long> getTopPosts(int limit) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        Set<Object> topPosts = zSetOps.reverseRange(POPULAR_KEY, 0, limit - 1);

        if (topPosts == null || topPosts.isEmpty()) return List.of();

        return topPosts.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toList());
    }

    public Map<Object, Object> getAllViewCounts() {
        return redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
    }

    public void deleteViewCount(Long postId) {
        redisTemplate.opsForZSet().remove(POPULAR_KEY, postId);
        if (hasViewCount(postId)) {
            redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, postId.toString());
        }
    }

    public void deleteViewCountKeys(Set<String> keys) {
        redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, keys.toArray());
    }
}
