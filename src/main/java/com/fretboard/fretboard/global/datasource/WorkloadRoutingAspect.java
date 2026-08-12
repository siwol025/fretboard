package com.fretboard.fretboard.global.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link Analytics} 가 붙은 메서드 실행 동안 {@link WorkloadContextHolder} 를
 * {@link ReadWorkload#ANALYTICS} 로 세팅하고, 종료 시(정상/예외 모두) 이전 값으로 복원한다.
 *
 * <p>실행 순서: {@code @Cacheable}(기본 {@link Ordered#LOWEST_PRECEDENCE} = 가장 안쪽)보다
 * 바깥에서 돌도록 {@code LOWEST_PRECEDENCE - 1} 로 지정한다. 워크로드 컨텍스트가 캐시 조회와
 * 메서드 본문을 모두 감싸므로, 캐시 미스로 DB 쿼리가 실행될 때 반드시 ANALYTICS 로 라우팅된다.
 * 캐시 히트 시엔 ThreadLocal set 1회만 발생하며(무해), 캐시는 RedisCacheManager 라 라우팅과 무관하다.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class WorkloadRoutingAspect {

    @Around("@annotation(com.fretboard.fretboard.global.datasource.Analytics)")
    public Object routeAnalytics(ProceedingJoinPoint joinPoint) throws Throwable {
        ReadWorkload previous = WorkloadContextHolder.getWorkload();
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);
        try {
            return joinPoint.proceed();
        } finally {
            WorkloadContextHolder.set(previous);
        }
    }
}
