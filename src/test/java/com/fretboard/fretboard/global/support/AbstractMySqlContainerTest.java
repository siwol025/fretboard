package com.fretboard.fretboard.global.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * 실제 MySQL 8.4(Testcontainers)에 대해 통합 테스트를 실행하기 위한 공통 베이스.
 * H2(MODE=MYSQL) 가 놓치는 MySQL 전용 비호환(예: WHERE id IN (...LIMIT) → ERROR 1235,
 * ALTER TABLE ... DROP INDEX 문법 등)을 실제 엔진에서 검증할 때 상속한다.
 *
 * <p>컨테이너는 <b>static 싱글톤</b>으로 한 번만 기동되어 여러 테스트 클래스가 재사용한다
 * (Testcontainers singleton 패턴). {@code @Container}(테스트 클래스마다 재기동) 대신
 * 정적 초기화 블록에서 {@code start()} 를 직접 호출하고, JVM 종료 시 Ryuk 이 컨테이너를 정리한다.
 *
 * <p>어노테이션 배치:
 * <ul>
 *   <li>{@code @AutoConfigureTestDatabase(replace = NONE)} — @DataJpaTest 의 임베디드 H2 교체를 비활성화한다.
 *       (상속되므로 하위 클래스에서 재선언 불필요)</li>
 *   <li>{@code @ServiceConnection} — MySQLContainer 의 url/username/password 를 자동 배선한다(Spring Boot 3.1+).</li>
 *   <li>{@code @TestPropertySource} — Flyway 를 활성화해 실제 MySQL 에 마이그레이션을 적용한다.</li>
 * </ul>
 *
 * 하위 클래스는 {@code @DataJpaTest} 만 선언하면 된다.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class AbstractMySqlContainerTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}
