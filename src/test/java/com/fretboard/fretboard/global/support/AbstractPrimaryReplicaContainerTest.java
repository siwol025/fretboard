package com.fretboard.fretboard.global.support;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 읽기/쓰기 분리 라우팅을 실제 MySQL 두 노드에서 검증하기 위한 공통 베이스.
 *
 * <p>Task 4 의 라우팅 검증은 <b>노드 간 실제 복제(replication)를 필요로 하지 않는다</b>.
 * server-id 가 서로 다른 두 개의 독립 MySQL 컨테이너를 띄우고, 트랜잭션 안에서
 * {@code SELECT @@server_id} 로 어느 노드에 쿼리가 도달했는지 확인한다.
 * (실제 복제는 Task 5 로 이연.)
 *
 * <p>컨테이너는 <b>static 싱글톤</b>으로 한 번만 기동한다(Testcontainers singleton 패턴).
 *
 * <p>전체 {@code @SpringBootTest} 컨텍스트는 부팅 시 {@code BoardInitializer}(CommandLineRunner)
 * 를 실행한다. 이는 {@code boardRepository.count()}(Spring Data 기본 = readOnly 트랜잭션 → REPLICA)
 * 와 {@code save()}(쓰기 트랜잭션 → PRIMARY) 를 수행하므로 <b>두 노드 모두</b> 스키마가 필요하다.
 * 복제가 없으므로 정적 블록에서 두 컨테이너를 각각 Flyway 로 마이그레이션한다.
 */
public abstract class AbstractPrimaryReplicaContainerTest {

    public static final long PRIMARY_SERVER_ID = 1L;
    public static final long REPLICA_SERVER_ID = 2L;

    static final MySQLContainer<?> PRIMARY = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fretboard")
            .withCommand("--server-id=1");

    static final MySQLContainer<?> REPLICA = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("fretboard")
            .withCommand("--server-id=2");

    static {
        PRIMARY.start();
        REPLICA.start();
        migrate(PRIMARY);
        migrate(REPLICA);
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

        // test 프로파일의 H2 전용 JPA 설정을 실제 MySQL 노드에 맞게 재정의한다.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
