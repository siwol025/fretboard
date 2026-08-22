package com.fretboard.fretboard.global.support;

import java.time.Duration;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 GTID 복제(replication)가 동작하는 primary/replica MySQL 두 노드 + Redis 를 기동하는 공통 베이스.
 *
 * <p>{@link AbstractPrimaryReplicaContainerTest} 와 달리 두 노드는 실제로 복제로 연결된다.
 * primary 에 쓰면 binlog 를 통해 replica 로 전파되며, {@link #stopReplication()} 로 복제를
 * 멈추면 replica 는 그 시점의 데이터에 "얼어붙는다"(복제 지연 시뮬레이션).
 *
 * <p>이 베이스는 read-your-writes 일관성(쓰기 직후 같은 사용자의 read-only 조회가 primary 로
 * 라우팅되어 방금 쓴 데이터를 보는지)을 복제 지연 상황에서 검증하기 위한 것이다.
 *
 * <p>컨테이너는 static 싱글톤으로 한 번만 기동한다(Testcontainers singleton 패턴).
 */
public abstract class AbstractReplicationContainerTest {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

    private static final String DB_NAME = "fretboard";
    private static final String REPL_USER = "repl";
    private static final String REPL_PASSWORD = "replpass";
    private static final String PRIMARY_ALIAS = "mysql-primary";

    static final Network NETWORK = Network.newNetwork();

    static final MySQLContainer<?> PRIMARY = new MySQLContainer<>(MYSQL_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases(PRIMARY_ALIAS)
            .withDatabaseName(DB_NAME)
            .withCommand(
                    "--server-id=1",
                    "--log-bin=mysql-bin",
                    "--gtid-mode=ON",
                    "--enforce-gtid-consistency=ON",
                    "--binlog-format=ROW",
                    "--log-replica-updates=ON");

    static final MySQLContainer<?> REPLICA = new MySQLContainer<>(MYSQL_IMAGE)
            .withNetwork(NETWORK)
            .withDatabaseName(DB_NAME)
            .withCommand(
                    "--server-id=2",
                    "--gtid-mode=ON",
                    "--enforce-gtid-consistency=ON");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withNetwork(NETWORK)
            .withExposedPorts(6379);

    private static JdbcTemplate primaryRootJdbc;
    private static JdbcTemplate replicaRootJdbc;

    static {
        PRIMARY.start();
        REPLICA.start();
        REDIS.start();

        primaryRootJdbc = rootJdbc(PRIMARY);
        replicaRootJdbc = rootJdbc(REPLICA);

        configureReplication();
        migratePrimary();
        awaitReplicaCaughtUp();
    }

    /**
     * root 계정 JDBC. Testcontainers 의 기본 사용자는 제한된 권한을 갖기 때문에
     * 복제 설정(CREATE USER, CHANGE REPLICATION SOURCE 등)은 root 로 수행해야 한다.
     */
    private static JdbcTemplate rootJdbc(MySQLContainer<?> container) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(container.getDriverClassName());
        ds.setUrl(container.getJdbcUrl());
        ds.setUsername("root");
        ds.setPassword(container.getPassword());
        return new JdbcTemplate(ds);
    }

    private static void configureReplication() {
        // 1) primary: 복제 계정을 binlog 에 남기지 않고 생성한다.
        //    (계정 생성이 binlog 에 들어가면 GTID auto-position 시 replica 에서 충돌한다.)
        primaryRootJdbc.execute("SET @@SESSION.SQL_LOG_BIN=0");
        primaryRootJdbc.execute(
                "CREATE USER '" + REPL_USER + "'@'%' IDENTIFIED BY '" + REPL_PASSWORD + "'");
        primaryRootJdbc.execute(
                "GRANT REPLICATION SLAVE ON *.* TO '" + REPL_USER + "'@'%'");
        primaryRootJdbc.execute("FLUSH PRIVILEGES");
        primaryRootJdbc.execute("SET @@SESSION.SQL_LOG_BIN=1");

        // 2) primary 가 부팅 중(Testcontainers 의 docker-entrypoint) 이미 실행해 binlog 에 남긴
        //    트랜잭션들(예: CREATE USER 'test'@'%')의 GTID 집합을 확보한다.
        //    이 트랜잭션들은 replica 도 자신의 entrypoint 에서 동일하게 수행했으므로,
        //    복제로 다시 적용하면 "user already exists" 등으로 충돌한다.
        String primaryExecutedGtids = primaryRootJdbc.queryForObject(
                "SELECT @@GLOBAL.gtid_executed", String.class);

        // 3) replica: 위 부팅 GTID 집합을 이미 적용한 것으로 간주(gtid_purged)하여 건너뛰고,
        //    이후의 새 트랜잭션(우리의 Flyway 마이그레이션 등)만 복제하도록 한다.
        replicaRootJdbc.execute("STOP REPLICA");
        replicaRootJdbc.execute("RESET REPLICA ALL");
        replicaRootJdbc.execute("RESET BINARY LOGS AND GTIDS");
        if (primaryExecutedGtids != null && !primaryExecutedGtids.isBlank()) {
            String purged = primaryExecutedGtids.replaceAll("\\s+", "");
            replicaRootJdbc.execute("SET @@GLOBAL.gtid_purged = '" + purged + "'");
        }

        // 4) replica: primary 를 소스로 지정하고 복제를 시작한다.
        //    SOURCE_PORT=3306 은 네트워크 alias 를 통한 컨테이너 내부 포트다(매핑 포트 아님).
        replicaRootJdbc.execute(
                "CHANGE REPLICATION SOURCE TO "
                        + "SOURCE_HOST='" + PRIMARY_ALIAS + "', "
                        + "SOURCE_PORT=3306, "
                        + "SOURCE_USER='" + REPL_USER + "', "
                        + "SOURCE_PASSWORD='" + REPL_PASSWORD + "', "
                        + "SOURCE_AUTO_POSITION=1, "
                        + "GET_SOURCE_PUBLIC_KEY=1");
        replicaRootJdbc.execute("START REPLICA");
    }

    private static void migratePrimary() {
        // primary 에만 마이그레이션한다. 스키마 DDL 은 binlog 를 통해 replica 로 복제된다.
        Flyway.configure()
                .dataSource(PRIMARY.getJdbcUrl(), PRIMARY.getUsername(), PRIMARY.getPassword())
                .load()
                .migrate();
    }

    /**
     * replica 가 primary 의 스키마(board 테이블)까지 복제를 따라잡을 때까지 대기한다.
     */
    protected static void awaitReplicaCaughtUp() {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            Integer count = replicaRootJdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema='" + DB_NAME + "' AND table_name='board'",
                    Integer.class);
            if (count != null && count == 1) {
                return;
            }
            sleep(200);
        }
        throw new IllegalStateException(
                "Replica did not catch up with primary schema within timeout. Replica status: "
                        + describeReplicaStatus() + " | Worker error: " + describeWorkerError());
    }

    private static String describeReplicaStatus() {
        try {
            return replicaRootJdbc.query("SHOW REPLICA STATUS", rs -> {
                if (!rs.next()) {
                    return "<no replica status rows>";
                }
                return "IO_Running=" + rs.getString("Replica_IO_Running")
                        + ", SQL_Running=" + rs.getString("Replica_SQL_Running")
                        + ", Last_IO_Error=" + rs.getString("Last_IO_Error")
                        + ", Last_SQL_Error=" + rs.getString("Last_SQL_Error")
                        + ", Source_Host=" + rs.getString("Source_Host");
            });
        } catch (RuntimeException e) {
            return "<failed to read replica status: " + e.getMessage() + ">";
        }
    }

    private static String describeWorkerError() {
        try {
            return String.valueOf(replicaRootJdbc.queryForList(
                    "SELECT WORKER_ID, LAST_ERROR_NUMBER, LAST_ERROR_MESSAGE "
                            + "FROM performance_schema.replication_applier_status_by_worker "
                            + "WHERE LAST_ERROR_MESSAGE <> ''"));
        } catch (RuntimeException e) {
            return "<failed to read worker error: " + e.getMessage() + ">";
        }
    }

    protected static void stopReplication() {
        replicaRootJdbc.execute("STOP REPLICA");
    }

    protected static void startReplication() {
        replicaRootJdbc.execute("START REPLICA");
    }

    /**
     * 주어진 조건이 replica 에서 참이 될 때까지(복제가 특정 데이터까지 따라잡을 때까지) 대기한다.
     */
    protected static void awaitOnReplica(String sqlReturningCount, Object[] args, long expected, long timeoutMillis) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(timeoutMillis));
        while (Instant.now().isBefore(deadline)) {
            Long actual = replicaRootJdbc.queryForObject(sqlReturningCount, Long.class, args);
            if (actual != null && actual == expected) {
                return;
            }
            sleep(100);
        }
        throw new IllegalStateException("Replica did not reach expected state within timeout: " + sqlReturningCount);
    }

    protected static JdbcTemplate primaryRootJdbc() {
        return primaryRootJdbc;
    }

    protected static JdbcTemplate replicaRootJdbc() {
        return replicaRootJdbc;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.primary.url", PRIMARY::getJdbcUrl);
        registry.add("datasource.primary.username", PRIMARY::getUsername);
        registry.add("datasource.primary.password", PRIMARY::getPassword);
        registry.add("datasource.primary.driver-class-name", PRIMARY::getDriverClassName);

        registry.add("datasource.replica.url", REPLICA::getJdbcUrl);
        registry.add("datasource.replica.username", REPLICA::getUsername);
        registry.add("datasource.replica.password", REPLICA::getPassword);
        registry.add("datasource.replica.driver-class-name", REPLICA::getDriverClassName);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
