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
        HikariDataSource dataSource = primaryDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("primary");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.replica.hikari")
    public HikariDataSource replicaDataSource(DataSourceProperties replicaDataSourceProperties) {
        HikariDataSource dataSource = replicaDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("replica");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("datasource.analytics")
    public DataSourceProperties analyticsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.analytics.hikari")
    public HikariDataSource analyticsDataSource(DataSourceProperties analyticsDataSourceProperties) {
        HikariDataSource dataSource = analyticsDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        dataSource.setPoolName("analytics");
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
