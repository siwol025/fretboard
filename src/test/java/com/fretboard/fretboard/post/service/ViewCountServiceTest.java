package com.fretboard.fretboard.post.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ViewCountServiceTest {

    @InjectMocks
    private ViewCountService viewCountService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private static final String VIEW_COUNT_KEY = "post:viewCount";

    @Test
    void setInitialViewCount_호출시_hash_put_실행() {
        // given
        Long postId = 1L;
        Long count = 100L;
        given(redisTemplate.opsForHash()).willReturn(hashOps);

        // when
        viewCountService.setInitialViewCount(postId, count);

        // then
        verify(hashOps).put(VIEW_COUNT_KEY, postId.toString(), count);
    }

    @Test
    void incrementViewCount_호출시_hash만_증분하고_zset은_건드리지_않음() {
        // given
        Long postId = 1L;
        given(redisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.increment(VIEW_COUNT_KEY, postId.toString(), 1)).willReturn(1L);

        // when
        viewCountService.incrementViewCount(postId);

        // then
        verify(hashOps).increment(VIEW_COUNT_KEY, postId.toString(), 1);
        verify(redisTemplate, never()).opsForZSet();
    }

    @Test
    void hasViewCount_키_존재시_true_반환() {
        // given
        Long postId = 1L;
        given(redisTemplate.opsForHash()).willReturn(hashOps);
        given(hashOps.hasKey(VIEW_COUNT_KEY, postId.toString())).willReturn(true);

        // when
        boolean result = viewCountService.hasViewCount(postId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void deleteViewCountKeys_지정된_키만_삭제된다() {
        // given
        Set<String> keys = Set.of("1", "2");
        given(redisTemplate.opsForHash()).willReturn(hashOps);

        // when
        viewCountService.deleteViewCountKeys(keys);

        // then
        verify(hashOps).delete(org.mockito.ArgumentMatchers.eq(VIEW_COUNT_KEY),
                org.mockito.ArgumentMatchers.any(Object[].class));
    }
}
