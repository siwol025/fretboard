package com.fretboard.fretboard.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import com.fretboard.fretboard.global.config.PopularProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // 2026-08-22 21:00 KST (12:00 UTC on 2026-08-22)
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), KST);

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOps;

    // 하드코딩 상수(view=1, like=3, comment=2, ttl=2d)와 다른 값으로 설정한다.
    private static final PopularProperties POPULAR_PROPERTIES = new PopularProperties(
            new PopularProperties.Weights(2.0, 5.0, 4.0), 10, 10, Duration.ofDays(7));

    private PopularScoreService popularScoreService;

    @BeforeEach
    void setUp() {
        popularScoreService = new PopularScoreService(redisTemplate, FIXED_CLOCK, POPULAR_PROPERTIES);
    }

    private String todayKey() {
        return "popular:today:" + LocalDate.now(FIXED_CLOCK);
    }

    @Test
    void applyView_설정된_가중치_사용() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyView(postId);

        // then — 설정된 view 가중치(2.0)로 증분되어야 한다
        verify(zSetOps).incrementScore(expectedKey, postId, 2.0);
    }

    @Test
    void applyLike_설정된_가중치_사용() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyLike(postId);

        // then — 설정된 like 가중치(5.0)로 증분되어야 한다
        verify(zSetOps).incrementScore(expectedKey, postId, 5.0);
    }

    @Test
    void applyComment_설정된_가중치_사용() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyComment(postId);

        // then — 설정된 comment 가중치(4.0)로 증분되어야 한다
        verify(zSetOps).incrementScore(expectedKey, postId, 4.0);
    }

    @Test
    void cancelLike_설정된_가중치_차감() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.cancelLike(postId);

        // then — 설정된 like 가중치(-5.0)로 차감되어야 한다
        verify(zSetOps).incrementScore(expectedKey, postId, -5.0);
    }

    @Test
    void cancelComment_설정된_가중치_차감() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.cancelComment(postId);

        // then — 설정된 comment 가중치(-4.0)로 차감되어야 한다
        verify(zSetOps).incrementScore(expectedKey, postId, -4.0);
    }

    @Test
    void expire_설정된_TTL_사용() {
        // given
        Long postId = 1L;
        String expectedKey = todayKey();
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        popularScoreService.applyView(postId);

        // then — 설정된 TTL(7일)로 expire 되어야 한다
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eq(expectedKey), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofDays(7));
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

    @Test
    void 자정_지나면_다른_날짜키_사용() {
        // given: 하루 차이가 나는 두 개의 고정 시각 (KST 기준 2026-08-22 vs 2026-08-23)
        Long postId = 1L;
        Clock clockDay22 = Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), KST);
        Clock clockDay23 = Clock.fixed(Instant.parse("2026-08-23T12:00:00Z"), KST);
        PopularScoreService serviceDay22 = new PopularScoreService(redisTemplate, clockDay22, POPULAR_PROPERTIES);
        PopularScoreService serviceDay23 = new PopularScoreService(redisTemplate, clockDay23, POPULAR_PROPERTIES);
        given(redisTemplate.opsForZSet()).willReturn(zSetOps);

        // when
        serviceDay22.applyLike(postId);
        serviceDay23.applyLike(postId);

        // then: 각각 해당 날짜가 포함된 서로 다른 키로 증분되어야 한다 (설정된 like 가중치 5.0)
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(zSetOps, org.mockito.Mockito.times(2))
                .incrementScore(keyCaptor.capture(), eq(postId), eq(5.0));
        assertThat(keyCaptor.getAllValues())
                .containsExactly("popular:today:2026-08-22", "popular:today:2026-08-23");
    }
}
