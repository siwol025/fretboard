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
 * Wires the read/write datasource split.
 *
 * <p>A {@link RoutingDataSource} chooses between the primary (write) and replica (read)
 * {@link HikariDataSource}s per transaction, and the whole thing is wrapped in a
 * {@link LazyConnectionDataSourceProxy} so the physical connection — and therefore the
 * routing key — is acquired only at the first statement, after the transaction's
 * read-only flag has been set.
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
        return primaryDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties("datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("datasource.replica.hikari")
    public HikariDataSource replicaDataSource(DataSourceProperties replicaDataSourceProperties) {
        return replicaDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public RoutingDataSource routingDataSource(
            ObjectProvider<WriteMarker> writeMarkerProvider,
            HikariDataSource primaryDataSource,
            HikariDataSource replicaDataSource) {
        RoutingDataSource routingDataSource = new RoutingDataSource(writeMarkerProvider);

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(RoutingDataSource.PRIMARY, primaryDataSource);
        targetDataSources.put(RoutingDataSource.REPLICA, replicaDataSource);

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
