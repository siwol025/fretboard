package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.fretboard.fretboard.global.support.AbstractReplicationContainerTest;
import com.fretboard.fretboard.member.dto.SignupRequest;
import com.fretboard.fretboard.member.service.MemberService;
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
 * read-your-writes 일관성 end-to-end 검증.
 *
 * <p>실제 GTID 복제가 연결된 primary/replica 두 MySQL 노드 위에서, 로그인 사용자가 실제
 * 서비스 메서드({@link MemberService#signup})로 쓰기를 수행한 뒤 <b>복제를 정지시켜 지연을
 * 시뮬레이션</b>한 상태에서:
 * <ul>
 *   <li>같은(마킹된) 사용자의 read-only 조회는 PRIMARY 로 라우팅되어 방금 쓴 데이터를 본다.</li>
 *   <li>마킹되지 않은 다른 사용자의 read-only 조회는 REPLICA 로 라우팅되어 stale 데이터를 본다.</li>
 * </ul>
 *
 * <p>쓰기는 반드시 {@code WriteMarkingAspect} 포인트컷({@code com.fretboard.fretboard..service..})
 * 에 매칭되는 실제 서비스 메서드를 통해야 aspect + 트랜잭션 순서가 진짜로 검증된다.
 * (WriteMarker 를 직접 호출해 마킹하지 않는다.)
 */
@SpringBootTest
@ActiveProfiles("test")
class ReadYourWritesIntegrationTest extends AbstractReplicationContainerTest {

    private static final String COUNT_MEMBER_BY_USERNAME =
            "SELECT COUNT(*) FROM member WHERE username = ?";

    private final MemberService memberService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnlyTx;

    @Autowired
    ReadYourWritesIntegrationTest(
            MemberService memberService,
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {
        this.memberService = memberService;
        // 애플리케이션 라우팅 DataSource(@Primary LazyConnectionDataSourceProxy) 를 그대로 사용한다.
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.readOnlyTx = new TransactionTemplate(transactionManager);
        this.readOnlyTx.setReadOnly(true);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void 쓰기_직후_같은사용자_readOnly조회_방금쓴글_보임() {
        long userId = 1000L;
        String username = "rywself";

        // 로그인 사용자로서 실제 서비스 쓰기 → member 행 생성 + aspect afterCommit 으로 사용자 마킹.
        UserContextHolder.set(userId);
        memberService.signup(new SignupRequest(username, "password1", "닉네임self"));

        // 복제를 정지시켜 replica 를 얼린다(복제 지연 시뮬레이션).
        // 이 시점 이후 primary 에만 있는 데이터는 replica 에서 보이지 않는다.
        stopReplication();
        try {
            // 여전히 같은(마킹된) 사용자로 read-only 조회.
            // 마킹되어 있으므로 PRIMARY 로 라우팅 → 방금 쓴 행이 보여야 한다.
            long count = readOnlyTx.execute(status ->
                    jdbcTemplate.queryForObject(COUNT_MEMBER_BY_USERNAME, Long.class, username));

            assertThat(count)
                    .as("마킹된 사용자의 read-only 조회는 PRIMARY 로 라우팅되어 방금 쓴 행을 본다")
                    .isEqualTo(1L);
        } finally {
            startReplication();
            awaitOnReplica(COUNT_MEMBER_BY_USERNAME, new Object[] {username}, 1L, 30_000L);
        }
    }

    @Test
    void 마킹없는_다른사용자_readOnly조회_stale() {
        long writerUserId = 3000L;
        long unmarkedReaderUserId = 2000L;
        String username = "stalerow";

        // 먼저 복제를 정지시킨 뒤 쓰기 → primary 에만 존재하고 replica 로는 전파되지 않는다.
        stopReplication();
        try {
            UserContextHolder.set(writerUserId);
            memberService.signup(new SignupRequest(username, "password1", "닉네임stale"));

            // 마킹되지 않은 다른 사용자로 read-only 조회 → REPLICA 로 라우팅.
            // replica 는 얼어 있으므로 방금 쓴 행이 보이지 않아야 한다(stale).
            UserContextHolder.set(unmarkedReaderUserId);
            long count = readOnlyTx.execute(status ->
                    jdbcTemplate.queryForObject(COUNT_MEMBER_BY_USERNAME, Long.class, username));

            assertThat(count)
                    .as("마킹되지 않은 사용자의 read-only 조회는 REPLICA(얼어붙음)로 라우팅되어 stale 하다")
                    .isEqualTo(0L);
        } finally {
            startReplication();
            awaitOnReplica(COUNT_MEMBER_BY_USERNAME, new Object[] {username}, 1L, 30_000L);
        }
    }
}
