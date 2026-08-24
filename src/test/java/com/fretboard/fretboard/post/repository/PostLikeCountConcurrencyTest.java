package com.fretboard.fretboard.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.support.AbstractMySqlContainerTest;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 동시성 증명(Concurrency Proof) 통합 테스트 — lost update 방지 회귀 가드.
 *
 * <p>서로 다른 N 명이 같은 글에 거의 동시에 좋아요를 누를 때, 비정규화된
 * {@code post.like_count} 가 정확히 N 이 되는지 검증한다. 증가는 원자적 SQL
 * {@code UPDATE post SET like_count = like_count + 1} (PostRepository#incrementLikeCount)
 * 로 수행되므로, 실제 MySQL 의 row-level lock 이 read-modify-write 경합을 직렬화해
 * lost update 가 발생하지 않아야 한다.
 *
 * <p>왜 실제 MySQL(Testcontainers)인가: H2(@DataJpaTest)로는 실제 행 잠금 기반의
 * 동시성 직렬화를 증명할 수 없다. {@link AbstractMySqlContainerTest} 의 싱글톤
 * MySQL 8.4 컨테이너 + Flyway 마이그레이션 위에서 실행한다.
 *
 * <p>왜 @SpringBootTest(비트랜잭션)인가: @SpringBootTest 는 @DataJpaTest 와 달리
 * 테스트 메서드를 롤백 트랜잭션으로 감싸지 않는다. 따라서 setup 저장이 COMMIT 되어
 * 별도 커넥션의 워커 스레드에서도 보이고, 각 워커의 증가도 자기 트랜잭션으로 독립 커밋된다.
 * 공유 컨테이너에 상태가 누수되지 않도록 @AfterEach 에서 생성 행을 명시적으로 삭제한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostLikeCountConcurrencyTest extends AbstractMySqlContainerTest {

    private static final int N = 50;
    // 풀 크기는 N 이상이어야 한다. 모든 태스크가 start 배리어에서 동시에 대기하려면
    // (ready.countDown 후 start.await 로 블로킹) N 개의 스레드가 동시에 살아 있어야 하며,
    // 풀이 더 작으면 앞선 태스크들이 배리어에서 스레드를 점유한 채 나머지가 시작조차 못 해
    // ready 래치가 0 에 도달하지 못하는 교착이 발생한다.
    private static final int THREAD_POOL_SIZE = N;

    /**
     * "test" 프로파일(application-test.yml)은 컨텍스트 로딩에 필요한 JWT/AWS/redis 등의
     * 값을 제공하지만 H2 datasource 와 H2Dialect 를 지정한다. 실제 datasource 는 상위
     * {@link AbstractMySqlContainerTest} 의 {@code @ServiceConnection}(JdbcConnectionDetails)
     * 이 MySQL 컨테이너로 재배선하므로 url/username/password/driver 는 컨테이너 값이 우선한다.
     * 다만 dialect 는 프로퍼티라 ServiceConnection 이 건드리지 않으므로 여기서 MySQL 로 강제한다.
     */
    @DynamicPropertySource
    static void overrideDialectToMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private Long postId;
    private Long boardId;
    private Long memberId;

    @AfterEach
    void tearDown() {
        // @SpringBootTest 는 비트랜잭션이므로 생성 행을 직접 정리해 컨테이너 상태 누수를 막는다.
        TransactionTemplate cleanup = new TransactionTemplate(transactionManager);
        cleanup.executeWithoutResult(status -> {
            if (postId != null) {
                entityManager.createQuery("DELETE FROM Post p WHERE p.id = :id")
                        .setParameter("id", postId)
                        .executeUpdate();
            }
            if (boardId != null) {
                entityManager.createQuery("DELETE FROM Board b WHERE b.id = :id")
                        .setParameter("id", boardId)
                        .executeUpdate();
            }
            if (memberId != null) {
                entityManager.createQuery("DELETE FROM Member m WHERE m.id = :id")
                        .setParameter("id", memberId)
                        .executeUpdate();
            }
        });
    }

    @Test
    void 서로_다른_사용자_동시_좋아요시_like_count가_정확히_N이_된다() throws InterruptedException {
        // given — Member/Board/Post 를 COMMIT 하여 워커 스레드에서 보이도록 한다 (like_count 는 0 으로 시작)
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            Member member = Member.builder()
                    .username("concurrency-user")
                    .password("password")
                    .nickname("concurrencynick")
                    .role(Role.USER)
                    .build();
            entityManager.persist(member);

            Board board = Board.builder()
                    .title("동시성 게시판")
                    .description("동시성 테스트 게시판")
                    .slug("concurrency-board")
                    .boardType(BoardType.WRITABLE)
                    .build();
            entityManager.persist(board);

            Post post = Post.builder()
                    .title("동시 좋아요 대상 글")
                    .content("본문")
                    .member(member)
                    .board(board)
                    .build();
            entityManager.persist(post);

            entityManager.flush();

            memberId = member.getId();
            boardId = board.getId();
            postId = post.getId();
        });

        assertThat(postId).as("setup 이 커밋되어 postId 가 확보되어야 한다").isNotNull();

        // when — N 개의 스레드가 최대한 동시에 각자의 트랜잭션으로 원자적 증가를 커밋한다
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch ready = new CountDownLatch(N);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(N);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < N; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                    txTemplate.executeWithoutResult(status -> postRepository.incrementLikeCount(postId));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.add(e);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        // 모든 태스크가 대기 지점에 도달할 때까지 기다린 뒤 동시에 발사
        boolean allReady = ready.await(30, TimeUnit.SECONDS);
        assertThat(allReady).as("모든 태스크가 시작 대기 지점에 도달해야 한다").isTrue();
        start.countDown();

        boolean allDone = done.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(allDone).as("모든 %d 개의 좋아요 태스크가 타임아웃 내에 완료되어야 한다", N).isTrue();
        assertThat(failures)
                .as("워커 스레드에서 예외가 발생하면 조용한 실패가 되지 않도록 표면화한다: %s", failures)
                .isEmpty();

        // then — 새 트랜잭션으로 최신 상태를 읽어 like_count 를 확인한다
        TransactionTemplate readTemplate = new TransactionTemplate(transactionManager);
        Long finalLikeCount = readTemplate.execute(status -> {
            entityManager.clear();
            Post fresh = postRepository.findById(postId).orElseThrow();
            return fresh.getLikeCount();
        });

        assertThat(finalLikeCount)
                .as("N=%d 명이 동시에 좋아요를 눌러도 원자적 UPDATE (like_count = like_count + 1) 가 "
                        + "row-level lock 으로 read-modify-write 를 직렬화하므로 lost update 없이 정확히 %d 여야 한다",
                        N, N)
                .isEqualTo((long) N);
    }
}
