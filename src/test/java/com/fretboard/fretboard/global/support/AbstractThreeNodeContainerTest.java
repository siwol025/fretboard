package com.fretboard.fretboard.global.support;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 3-way 워크로드 라우팅(primary/replica/analytics)을 실제 MySQL 세 노드에서 검증하기 위한 공통 베이스.
 *
 * <p>{@link AbstractPrimaryReplicaContainerTest} 를 본떠 server-id=3 인 ANALYTICS 노드를 추가한다.
 * 노드 간 실제 복제(replication)는 필요하지 않으며, server-id 가 서로 다른 세 개의 독립 MySQL
 * 컨테이너를 띄우고 트랜잭션 안에서 {@code SELECT @@server_id} 로 어느 노드에 쿼리가 도달했는지 확인한다.
 *
 * <p>컨테이너는 <b>static 싱글톤</b>으로 한 번만 기동하며, 복제가 없으므로 각 컨테이너를 각각
 * Flyway 로 마이그레이션한다.
 */
public abstract class AbstractThreeNodeContainerTest {

    public static final long PRIMARY_SERVER_ID = 1L;
    public static final long REPLICA_SERVER_ID = 2L;
    public static final long ANALYTICS_SERVER_ID = 3L;

    static final MySQLContainer<?> PRIMARY = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fretboard")
            .withCommand("--server-id=1");

    static final MySQLContainer<?> REPLICA = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fretboard")
            .withCommand("--server-id=2");

    static final MySQLContainer<?> ANALYTICS = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fretboard")
            .withCommand("--server-id=3");

    static {
        PRIMARY.start();
        REPLICA.start();
        ANALYTICS.start();
        migrate(PRIMARY);
        migrate(REPLICA);
        migrate(ANALYTICS);
    }

    private static void migrate(MySQLContainer<?> container) {
        Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void registerDataSources(DynamicPropertyRegistry registry) {
        registry.add("datasource.primary.url", PRIMARY::getJdbcUrl);
        registry.add("datasource.primary.username", PRIMARY::getUsername);
        registry.add("datasource.primary.password", PRIMARY::getPassword);
        registry.add("datasource.primary.driver-class-name", PRIMARY::getDriverClassName);

        registry.add("datasource.replica.url", REPLICA::getJdbcUrl);
        registry.add("datasource.replica.username", REPLICA::getUsername);
        registry.add("datasource.replica.password", REPLICA::getPassword);
        registry.add("datasource.replica.driver-class-name", REPLICA::getDriverClassName);

        registry.add("datasource.analytics.url", ANALYTICS::getJdbcUrl);
        registry.add("datasource.analytics.username", ANALYTICS::getUsername);
        registry.add("datasource.analytics.password", ANALYTICS::getPassword);
        registry.add("datasource.analytics.driver-class-name", ANALYTICS::getDriverClassName);

        // test 프로파일의 H2 전용 JPA 설정을 실제 MySQL 노드에 맞게 재정의한다.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
