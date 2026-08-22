package com.fretboard.fretboard.global.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * Wires the workload-class datasource routing.
 *
 * <p>A {@link RoutingDataSource} chooses between the primary (write + read-your-writes),
 * replica (SERVING reads: feed/detail), and analytics (heavy aggregation/search reads)
 * {@link HikariDataSource}s per transaction, and the whole thing is wrapped in a
 * {@link LazyConnectionDataSourceProxy} so the physical connection — and therefore the
 * routing key — is acquired only at the first statement, after the transaction's
 * read-only flag and workload class have been set.
 *
 * <p>Each pool is named ({@code primary}/{@code replica}/{@code analytics}) so per-node
 * HikariCP metrics are identifiable (e.g. {@code hikaricp_connections_active{pool="analytics"}}).
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.primary.hikari")
    public HikariDataSource primaryDataSource(DataSourceProperties primaryDataSourceProperties) {
        return buildPool(primaryDataSourceProperties, RoutingDataSource.PRIMARY);
    }

    @Bean
    @ConfigurationProperties("datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.replica.hikari")
    public HikariDataSource replicaDataSource(DataSourceProperties replicaDataSourceProperties) {
        return buildPool(replicaDataSourceProperties, RoutingDataSource.REPLICA);
    }

    @Bean
    @ConfigurationProperties("datasource.analytics")
    public DataSourceProperties analyticsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.analytics.hikari")
    public HikariDataSource analyticsDataSource(DataSourceProperties analyticsDataSourceProperties) {
        return buildPool(analyticsDataSourceProperties, RoutingDataSource.ANALYTICS);
    }

    /**
     * {@link DataSourceProperties} 로부터 이름 붙은 {@link HikariDataSource} 를 조립한다.
     * pool-name 은 라우팅 키 상수를 그대로 써(예: {@code hikaricp_connections_active{pool="analytics"}})
     * 노드별 메트릭 식별과 라우팅 키를 일치시킨다. {@code @ConfigurationProperties("datasource.*.hikari")}
     * 로 바인딩되는 hikari 속성(예: rewriteBatchedStatements)은 반환 후 후처리로 적용된다.
     */
    private HikariDataSource buildPool(DataSourceProperties properties, String poolName) {
        HikariDataSource dataSource = properties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName(poolName);
        return dataSource;
    }

    @Bean
    public RoutingDataSource routingDataSource(
            ObjectProvider<WriteMarker> writeMarkerProvider,
            HikariDataSource primaryDataSource,
            HikariDataSource replicaDataSource,
            HikariDataSource analyticsDataSource) {
        RoutingDataSource routingDataSource = new RoutingDataSource(writeMarkerProvider);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(RoutingDataSource.PRIMARY, primaryDataSource);
        targetDataSources.put(RoutingDataSource.REPLICA, replicaDataSource);
        targetDataSources.put(RoutingDataSource.ANALYTICS, analyticsDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        return routingDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(RoutingDataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}
