package com.fretboard.fretboard.post.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PopularScoreServiceTest {

    @InjectMocks
    private PopularScoreService popularScoreService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    private String todayKey() {
        return "popular:today:" + LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void applyView_오늘자ZSet_가중치1_증분_및_TTL설정() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyView(postId);

        // then
        verify(zSetOps).incrementScore(expectedKey, postId, 1.0);
        verify(redisTemplate).expire(eq(expectedKey), any(Duration.class));
    }

    @Test
    void applyLike_가중치3_증분() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyLike(postId);

        // then
        verify(zSetOps).incrementScore(expectedKey, postId, 3.0);
    }

    @Test
    void applyComment_가중치2_증분() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyComment(postId);

        // then
        verify(zSetOps).incrementScore(expectedKey, postId, 2.0);
    }

    @Test
    void cancelLike_가중치3_차감() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.cancelLike(postId);

        // then
        verify(zSetOps).incrementScore(expectedKey, postId, -3.0);
    }

    @Test
    void cancelComment_가중치2_차감() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.cancelComment(postId);

        // then
        verify(zSetOps).incrementScore(expectedKey, postId, -2.0);
    }

    @Test
    void getTopPostIds_상위N_postId리스트_반환() {
        // given
        int limit = 3;
        String expectedKey = todayKey();
        Set<Object> ranked = new LinkedHashSet<>();
        ranked.add("10");
        ranked.add("20");
        ranked.add("30");
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);
        given(zSetOps.reverseRange(expectedKey, 0, limit - 1)).willReturn(ranked);

        // when
        List<Long> result = popularScoreService.getTopPostIds(limit);

        // then
        assertThat(result).containsExactly(10L, 20L, 30L);
    }

    @Test
    void getTopPostIds_결과없으면_빈리스트() {
        // given
        int limit = 3;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);
        given(zSetOps.reverseRange(expectedKey, 0, limit - 1)).willReturn(null);

        // when
        List<Long> result = popularScoreService.getTopPostIds(limit);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void applyLike_Redis예외시_예외_전파하지_않음() {
        // given
        Long postId = 1L;
        given(redisTemplate.opsForZSet()).willThrow(new RuntimeException("redis down"));

        // when & then
        assertThatCode(() -> popularScoreService.applyLike(postId)).doesNotThrowAnyException();
    }
}
