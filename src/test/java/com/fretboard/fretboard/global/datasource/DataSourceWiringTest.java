package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.test.context.ActiveProfiles;

/**
 * Pins the critical wiring of the read/write datasource split:
 * the app's primary {@link DataSource} must be a {@link LazyConnectionDataSourceProxy}
 * wrapping a {@link RoutingDataSource}. The lazy proxy defers acquiring the physical
 * connection (and therefore the routing key) until the first statement — after the
 * transaction's readOnly flag is set. Without it, the connection is bound at
 * transaction start and the read/write routing silently breaks.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataSourceWiringTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void primaryDataSourceIsLazyProxyWrappingRoutingDataSource() {
        assertThat(dataSource)
                .as("primary DataSource must be a LazyConnectionDataSourceProxy")
                .isInstanceOf(LazyConnectionDataSourceProxy.class);

        DataSource target = ((LazyConnectionDataSourceProxy) dataSource).getTargetDataSource();
        assertThat(target)
                .as("lazy proxy must wrap the RoutingDataSource")
                .isInstanceOf(RoutingDataSource.class);

        assertThat(((RoutingDataSource) target).getResolvedDataSources())
                .as("routing must resolve all three workload targets")
                .containsKeys(RoutingDataSource.PRIMARY, RoutingDataSource.REPLICA, RoutingDataSource.ANALYTICS);
    }
}
