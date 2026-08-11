package com.fretboard.fretboard.global.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * {@link Analytics} 가 붙은 메서드 실행 동안 {@link WorkloadContextHolder} 를
 * {@link ReadWorkload#ANALYTICS} 로 세팅하고, 종료 시(정상/예외 모두) 이전 값으로 복원한다.
 */
@Aspect
@Component
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
