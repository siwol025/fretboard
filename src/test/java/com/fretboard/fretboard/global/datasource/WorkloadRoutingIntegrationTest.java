package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.fretboard.fretboard.global.support.AbstractThreeNodeContainerTest;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 3-way 워크로드 라우팅(primary/replica/analytics)을 실제 MySQL 세 노드에서 end-to-end 로 검증한다.
 *
 * <p>트랜잭션의 readOnly 플래그, 워크로드({@link ReadWorkload}), 사용자 컨텍스트에 따라
 * {@code SELECT @@server_id} 가 어느 물리 노드에 도달하는지 확인한다.
 * server-id=1 → PRIMARY, server-id=2 → REPLICA, server-id=3 → ANALYTICS.
 */
@SpringBootTest
@ActiveProfiles("test")
class WorkloadRoutingIntegrationTest extends AbstractThreeNodeContainerTest {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    WorkloadRoutingIntegrationTest(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        WorkloadContextHolder.clear();
    }

    @Test
    void 쓰기_트랜잭션은_primary_노드() {
        transactionTemplate.setReadOnly(false);

        Long serverId = transactionTemplate.execute(status -> queryServerId());

        assertThat(serverId).isEqualTo(PRIMARY_SERVER_ID);
    }

    @Test
    void SERVING_읽기는_replica_노드() {
        transactionTemplate.setReadOnly(true);

        Long serverId = transactionTemplate.execute(status -> queryServerId());

        assertThat(serverId).isEqualTo(REPLICA_SERVER_ID);
    }

    @Test
    void ANALYTICS_읽기는_analytics_노드() {
        transactionTemplate.setReadOnly(true);
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);

        Long serverId = transactionTemplate.execute(status -> queryServerId());

        assertThat(serverId).isEqualTo(ANALYTICS_SERVER_ID);
    }

    @Test
    void ANALYTICS는_최근쓰기_마킹무시하고_analytics_노드() {
        transactionTemplate.setReadOnly(true);
        UserContextHolder.set(1L);
        WorkloadContextHolder.set(ReadWorkload.ANALYTICS);

        Long serverId = transactionTemplate.execute(status -> queryServerId());

        assertThat(serverId).isEqualTo(ANALYTICS_SERVER_ID);
    }

    private Long queryServerId() {
        return jdbcTemplate.queryForObject("SELECT @@server_id", Long.class);
    }
}
