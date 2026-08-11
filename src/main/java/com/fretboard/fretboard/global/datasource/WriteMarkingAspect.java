package com.fretboard.fretboard.global.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 쓰기 트랜잭션이 커밋된 뒤 현재 사용자를 "최근 쓰기" 로 마킹하여,
 * 이어지는 읽기 전용 조회가 primary 로 라우팅되도록(read-your-writes) 한다.
 */
@Aspect
@Component
public class WriteMarkingAspect {

    private final WriteMarker writeMarker;

    @Value("${read-write-split.sticky-to-primary.enabled:true}")
    private boolean stickyToPrimaryEnabled = true;

    public WriteMarkingAspect(WriteMarker writeMarker) {
        this.writeMarker = writeMarker;
    }

    void setStickyToPrimaryEnabled(boolean stickyToPrimaryEnabled) {
        this.stickyToPrimaryEnabled = stickyToPrimaryEnabled;
    }

    @Around("execution(* com.fretboard.fretboard..service..*(..))")
    public Object markWriteAfterCommit(ProceedingJoinPoint joinPoint) throws Throwable {
        registerMarkingIfWrite();
        return joinPoint.proceed();
    }

    void registerMarkingIfWrite() {
        if (!stickyToPrimaryEnabled) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return;
        }
        final Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                writeMarker.mark(userId);
            }
        });
    }
}
