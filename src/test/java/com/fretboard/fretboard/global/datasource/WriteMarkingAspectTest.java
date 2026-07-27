package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link WriteMarkingAspect#registerMarkingIfWrite()} 의 등록/마킹 규칙을 검증한다.
 *
 * <p>트랜잭션 상태는 {@link TransactionSynchronizationManager} 로 직접 제어하고,
 * 사용자 컨텍스트는 {@link UserContextHolder}, 마킹 대상은 {@link WriteMarker}(Mock)로 제어한다.
 * 실제 AOP 프록시 없이 결정+등록 메서드를 직접 호출해 단위 테스트한다.
 */
class WriteMarkingAspectTest {

    private static final Long USER_ID = 1L;

    private WriteMarker writeMarker;
    private WriteMarkingAspect aspect;

    @BeforeEach
    void setUp() {
        writeMarker = mock(WriteMarker.class);
        aspect = new WriteMarkingAspect(writeMarker);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        TransactionSynchronizationManager.setActualTransactionActive(false);
        TransactionSynchronizationManager.clearSynchronization();
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("쓰기 트랜잭션이 커밋되면 현재 사용자를 마킹한다")
    void 쓰기트랜잭션_커밋후_마킹() {
        // given — 쓰기 트랜잭션 활성 + 사용자 존재
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        UserContextHolder.set(USER_ID);

        // when
        aspect.registerMarkingIfWrite();

        // then — 정확히 하나의 동기화가 등록된다
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);

        // when — 커밋 콜백 실행
        synchronizations.get(0).afterCommit();

        // then — 마킹 발생
        verify(writeMarker).mark(USER_ID);
    }

    @Test
    @DisplayName("롤백되면 마킹하지 않는다")
    void 롤백시_마킹안함() {
        // given — 쓰기 트랜잭션 활성 + 사용자 존재 + 등록
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        UserContextHolder.set(USER_ID);
        aspect.registerMarkingIfWrite();

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(synchronizations).hasSize(1);

        // when — 커밋이 아닌 롤백 완료만 실행
        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        // then — 마킹 없음
        verify(writeMarker, never()).mark(any());
    }

    @Test
    @DisplayName("읽기 전용 트랜잭션은 마킹하지 않는다")
    void readOnly_트랜잭션은_마킹안함() {
        // given — 읽기 전용 트랜잭션 + 사용자 존재
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        UserContextHolder.set(USER_ID);

        // when
        aspect.registerMarkingIfWrite();

        // then — 동기화 미등록
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    @DisplayName("사용자가 없으면 마킹하지 않는다")
    void 사용자없으면_마킹안함() {
        // given — 쓰기 트랜잭션 활성 + 사용자 없음
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        UserContextHolder.clear();

        // when
        aspect.registerMarkingIfWrite();

        // then — 동기화 미등록
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    @DisplayName("실제 트랜잭션이 없으면 마킹하지 않는다")
    void 트랜잭션없으면_마킹안함() {
        // given — 트랜잭션 미활성 + 사용자 존재
        TransactionSynchronizationManager.setActualTransactionActive(false);
        UserContextHolder.set(USER_ID);

        // when
        aspect.registerMarkingIfWrite();

        // then — 동기화 미등록
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }
}
