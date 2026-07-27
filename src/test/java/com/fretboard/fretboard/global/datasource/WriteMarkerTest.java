package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link RedisWriteMarker} 의 Redis 연동 계약을 검증한다.
 *
 * <p>{@code mark(userId)} 는 {@code user:{userId}:recentWrite} 키를 양수 TTL 과 함께 저장하고,
 * {@code hasRecentWrite(userId)} 는 키 존재 여부를 반영한다. Redis 예외 시 {@code mark} 는
 * best-effort/fail-open 으로 전파하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WriteMarkerTest {

    private static final Long USER_ID = 1L;
    private static final String RECENT_WRITE_KEY = "user:1:recentWrite";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private RedisWriteMarker writeMarker;

    @BeforeEach
    void setUp() {
        writeMarker = new RedisWriteMarker(redisTemplate);
    }

    @Test
    @DisplayName("mark 시 recentWrite 키를 TTL과 함께 저장")
    void mark_시_recentWrite_키를_TTL과_함께_저장() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        // when
        writeMarker.mark(USER_ID);

        // then
        verify(valueOps).set(eq(RECENT_WRITE_KEY), any(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isNotNull();
        assertThat(ttlCaptor.getValue().toMillis()).isPositive();
    }

    @Test
    @DisplayName("hasRecentWrite 키 존재시 true")
    void hasRecentWrite_키_존재시_true() {
        // given
        given(redisTemplate.hasKey(RECENT_WRITE_KEY)).willReturn(true);

        // when
        boolean result = writeMarker.hasRecentWrite(USER_ID);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasRecentWrite 키 없으면 false")
    void hasRecentWrite_키_없으면_false() {
        // given
        given(redisTemplate.hasKey(RECENT_WRITE_KEY)).willReturn(false);

        // when
        boolean result = writeMarker.hasRecentWrite(USER_ID);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("mark Redis 예외시 예외전파 안함")
    void mark_Redis_예외시_예외전파_안함() {
        // given — Redis 쓰기가 예외를 던짐
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        doThrow(new RuntimeException("redis down"))
                .when(valueOps)
                .set(eq(RECENT_WRITE_KEY), any(), any(Duration.class));

        // when & then — best-effort/fail-open: 예외를 전파하지 않는다
        assertThatCode(() -> writeMarker.mark(USER_ID)).doesNotThrowAnyException();
    }
}
