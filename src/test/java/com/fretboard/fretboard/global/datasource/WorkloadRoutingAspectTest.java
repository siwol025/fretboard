package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link WorkloadRoutingAspect#routeAnalytics(ProceedingJoinPoint)} 의
 * 워크로드 세팅/복원 규칙을 검증한다.
 *
 * <p>실제 AOP 프록시 없이 애스펙트를 직접 인스턴스화하고,
 * {@link ProceedingJoinPoint} 는 Mockito mock 으로 만들어 advice 를 직접 호출한다.
 * proceed() 시점의 {@link WorkloadContextHolder#getWorkload()} 값을 캡처해 검증한다.
 */
class WorkloadRoutingAspectTest {

    private WorkloadRoutingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new WorkloadRoutingAspect();
    }

    @AfterEach
    void tearDown() {
        WorkloadContextHolder.clear();
    }

    @Test
    @DisplayName("Analytics 메서드 실행 중에는 ANALYTICS 로 세팅된다")
    void Analytics_메서드_실행중_ANALYTICS_세팅() throws Throwable {
        // given — proceed 시점의 워크로드를 캡처
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        ReadWorkload[] captured = new ReadWorkload[1];
        when(pjp.proceed()).thenAnswer(inv -> {
            captured[0] = WorkloadContextHolder.getWorkload();
            return null;
        });

        // when
        aspect.routeAnalytics(pjp);

        // then — proceed 실행 시점에 ANALYTICS
        assertThat(captured[0]).isEqualTo(ReadWorkload.ANALYTICS);
    }

    @Test
    @DisplayName("메서드 종료 후에는 기본(SERVING)으로 정리된다")
    void 메서드_종료후_정리() throws Throwable {
        // given — proceed 동안 ANALYTICS 세팅됨을 함께 확인해 no-op 스텁과 구분한다
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        ReadWorkload[] captured = new ReadWorkload[1];
        when(pjp.proceed()).thenAnswer(inv -> {
            captured[0] = WorkloadContextHolder.getWorkload();
            return null;
        });

        // when
        aspect.routeAnalytics(pjp);

        // then — 실행 중엔 ANALYTICS, 종료 후엔 기본값 SERVING 으로 정리
        assertThat(captured[0]).isEqualTo(ReadWorkload.ANALYTICS);
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.SERVING);
    }

    @Test
    @DisplayName("예외가 발생해도 finally 로 정리하고 예외를 전파한다")
    void 예외_발생해도_finally_정리() throws Throwable {
        // given — proceed 진입 시 ANALYTICS 세팅 여부를 캡처한 뒤 예외를 던짐
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        RuntimeException boom = new RuntimeException("boom");
        ReadWorkload[] captured = new ReadWorkload[1];
        when(pjp.proceed()).thenAnswer(inv -> {
            captured[0] = WorkloadContextHolder.getWorkload();
            throw boom;
        });

        // when & then — 예외 전파
        assertThatThrownBy(() -> aspect.routeAnalytics(pjp)).isSameAs(boom);

        // then — 실행 중엔 ANALYTICS, 예외 후에도 finally 로 SERVING 복원
        assertThat(captured[0]).isEqualTo(ReadWorkload.ANALYTICS);
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.SERVING);
    }

    @Test
    @DisplayName("중첩 호출 시 종료 후 이전 값(ANALYTICS)을 복원한다")
    void 중첩호출시_이전값_복원() throws Throwable {
        // given — 진입 전 이미 ANALYTICS 로 세팅됨. proceed 동안엔 ANALYTICS 유지 확인
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        ReadWorkload[] captured = new ReadWorkload[1];
        when(pjp.proceed()).thenAnswer(inv -> {
            captured[0] = WorkloadContextHolder.getWorkload();
            return null;
        });

        // when
        aspect.routeAnalytics(pjp);

        // then — 실행 중 ANALYTICS, 종료 후 SERVING 으로 덮어쓰지 않고 이전 값 ANALYTICS 복원
        assertThat(captured[0]).isEqualTo(ReadWorkload.ANALYTICS);
        assertThat(WorkloadContextHolder.getWorkload()).isEqualTo(ReadWorkload.ANALYTICS);
    }
}
