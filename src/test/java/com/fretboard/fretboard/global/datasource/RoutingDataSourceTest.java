package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link RoutingDataSource#determineCurrentLookupKey()} 의 라우팅 규칙을 검증한다.
 *
 * <p>트랜잭션 상태는 {@link TransactionSynchronizationManager} 로 직접 제어하고,
 * 사용자 컨텍스트는 {@link UserContextHolder}, 최근 쓰기 마킹은 {@link WriteMarker}(Mock)로 제어한다.
 * {@code determineCurrentLookupKey()} 는 대상 데이터소스 맵을 읽지 않으므로
 * {@code afterPropertiesSet()} 없이 인스턴스만 생성해 직접 호출한다.
 */
class RoutingDataSourceTest {

    private static final String PRIMARY = "primary";
    private static final String REPLICA = "replica";
    private static final Long USER_ID = 42L;

    @AfterEach
    void resetState() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        TransactionSynchronizationManager.setActualTransactionActive(false);
        UserContextHolder.clear();
    }

    private RoutingDataSource routingDataSourceWith(WriteMarker marker) {
        @SuppressWarnings("unchecked")
        ObjectProvider<WriteMarker> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(marker);
        return new RoutingDataSource(provider);
    }

    private void activeTransaction(boolean readOnly) {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);
    }

    @Test
    @DisplayName("실제 트랜잭션이 없으면(부트/Flyway/validate) primary 로 라우팅한다")
    void 트랜잭션_없음_primary() {
        // given — 실제 트랜잭션 미활성
        TransactionSynchronizationManager.setActualTransactionActive(false);
        RoutingDataSource routingDataSource = routingDataSourceWith(mock(WriteMarker.class));

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then
        assertThat(key).isEqualTo(PRIMARY);
    }

    @Test
    @DisplayName("실제 트랜잭션이 있지만 읽기 전용이 아니면(쓰기) primary 로 라우팅한다")
    void readOnly_false_primary() {
        // given — 트랜잭션 활성 + 쓰기
        activeTransaction(false);
        RoutingDataSource routingDataSource = routingDataSourceWith(mock(WriteMarker.class));

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then
        assertThat(key).isEqualTo(PRIMARY);
    }

    @Test
    @DisplayName("읽기 전용 + 사용자 없음(익명 조회)이면 replica 로 라우팅한다")
    void readOnly_true_사용자없음_replica() {
        // given — 읽기 전용 트랜잭션 + 사용자 컨텍스트 없음
        activeTransaction(true);
        UserContextHolder.clear();
        RoutingDataSource routingDataSource = routingDataSourceWith(mock(WriteMarker.class));

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then
        assertThat(key).isEqualTo(REPLICA);
    }

    @Test
    @DisplayName("읽기 전용 + 사용자 있음 + 최근 쓰기 마킹 없음이면 replica 로 라우팅한다")
    void readOnly_true_마킹없음_replica() {
        // given — 읽기 전용 + 사용자 존재 + 최근 쓰기 없음
        activeTransaction(true);
        UserContextHolder.set(USER_ID);
        WriteMarker marker = mock(WriteMarker.class);
        when(marker.hasRecentWrite(USER_ID)).thenReturn(false);
        RoutingDataSource routingDataSource = routingDataSourceWith(marker);

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then
        assertThat(key).isEqualTo(REPLICA);
    }

    @Test
    @DisplayName("읽기 전용 + 사용자 있음 + 최근 쓰기 마킹 있음이면 primary 로 라우팅한다")
    void readOnly_true_최근쓰기_마킹있음_primary() {
        // given — 읽기 전용 + 사용자 존재 + 최근 쓰기 있음
        activeTransaction(true);
        UserContextHolder.set(USER_ID);
        WriteMarker marker = mock(WriteMarker.class);
        when(marker.hasRecentWrite(USER_ID)).thenReturn(true);
        RoutingDataSource routingDataSource = routingDataSourceWith(marker);

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then
        assertThat(key).isEqualTo(PRIMARY);
    }

    @Test
    @DisplayName("마커가 지연 주입으로 아직 준비되지 않았으면(getIfAvailable()==null) 안전하게 primary 로 폴백한다")
    void 마커_지연주입_미준비_시_primary_폴백() {
        // given — 읽기 전용 + 사용자 존재, 그러나 provider 가 마커를 아직 못 준다
        activeTransaction(true);
        UserContextHolder.set(USER_ID);
        RoutingDataSource routingDataSource = routingDataSourceWith(null);

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then — 최근성 판단 불가 → 안전 폴백
        assertThat(key).isEqualTo(PRIMARY);
    }

    @Test
    @DisplayName("마킹 조회 중 예외가 나면 삼키고 replica 로 fail-open 한다")
    void 마킹조회_예외_시_fail_open_replica() {
        // given — 읽기 전용 + 사용자 존재, 마커 조회가 예외를 던짐
        activeTransaction(true);
        UserContextHolder.set(USER_ID);
        WriteMarker marker = mock(WriteMarker.class);
        when(marker.hasRecentWrite(USER_ID)).thenThrow(new RuntimeException("redis down"));
        RoutingDataSource routingDataSource = routingDataSourceWith(marker);

        // when
        Object key = routingDataSource.determineCurrentLookupKey();

        // then — 일관성은 저하되어도 읽기는 살아있게 fail-open
        assertThat(key).isEqualTo(REPLICA);
    }
}
