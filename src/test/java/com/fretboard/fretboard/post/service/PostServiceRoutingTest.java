package com.fretboard.fretboard.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.datasource.UserContextHolder;
import com.fretboard.fretboard.global.support.AbstractReplicationContainerTest;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.member.repository.MemberRepository;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code PostService.getPostDetail} 의 읽기/쓰기 DataSource 라우팅 검증.
 *
 * <p>{@code getPostDetail} 은 DB 쓰기를 하지 않는다(유일한 mutation 인 조회수 증가는 Redis 다).
 * 따라서 read-only 트랜잭션으로 분류되어야 replica 로 오프로딩된다. 현재는 {@code @Transactional}
 * (non-readOnly) 이라 항상 primary 로 라우팅되므로 시나리오 1 이 RED 로 실패한다.
 *
 * <p>{@link ReadYourWritesIntegrationTest} 와 동일한 전략을 따른다: primary 에 데이터를 심고
 * replica 로 복제된 것을 확인한 뒤 {@link #stopReplication()} 으로 replica 를 얼려, 어느 노드가
 * 읽기를 처리했는지 stale/fresh 값으로 구분한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class PostServiceRoutingTest extends AbstractReplicationContainerTest {

    private static final String COUNT_POST_BY_TITLE = "SELECT COUNT(*) FROM post WHERE title = ?";
    private static final long REPLICATION_TIMEOUT_MILLIS = 30_000L;
    private static final long EXPECTED_ONE_ROW = 1L;

    private final PostService postService;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final TransactionTemplate writeTx;

    @Autowired
    PostServiceRoutingTest(
            PostService postService,
            MemberRepository memberRepository,
            BoardRepository boardRepository,
            PostRepository postRepository,
            PlatformTransactionManager transactionManager) {
        this.postService = postService;
        this.memberRepository = memberRepository;
        this.boardRepository = boardRepository;
        this.postRepository = postRepository;
        // 쓰기 트랜잭션(non-readOnly) → 라우팅 DataSource 가 primary 로 보낸다. 시드 데이터는 primary 에 안착 후 복제된다.
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void getPostDetail_readOnly로_replica_라우팅() {
        String oldTitle = "라우팅-old-title-" + System.nanoTime();
        String freshTitle = "라우팅-fresh-title-" + System.nanoTime();

        // 애플리케이션 라우팅 DataSource 로 board+member+post 를 primary 에 심는다(제목=old).
        Seed seed = seedPost(oldTitle);
        long postId = seed.postId;

        // post(old 제목)가 replica 로 복제될 때까지 대기.
        awaitOnReplica(COUNT_POST_BY_TITLE, new Object[] {oldTitle}, EXPECTED_ONE_ROW, REPLICATION_TIMEOUT_MILLIS);

        // 복제를 정지시켜 replica 를 얼린다. 이후 primary 변경은 replica 로 전파되지 않는다.
        stopReplication();
        try {
            // primary 에서만 제목을 fresh 로 갱신한다(root JDBC 직접 UPDATE → 얼어붙은 replica 에는 없음).
            primaryRootJdbc().update("UPDATE post SET title = ? WHERE id = ?", freshTitle, postId);

            // 마킹되지 않은 익명 리더로 상세 조회. read-only 라면 REPLICA(얼어붙음) 로 라우팅 → stale(old) 제목을 봐야 한다.
            UserContextHolder.clear();
            PostDetailResponse response = postService.getPostDetail(postId, null);

            assertThat(response.title())
                    .as("getPostDetail 은 read-only 로 분류되어 REPLICA(얼어붙음)로 라우팅되므로 stale(old) 제목을 본다")
                    .isEqualTo(oldTitle);
        } finally {
            // primary 의 fresh 제목이 replica 로 따라잡히게 하고 복제 재개.
            startReplication();
            awaitOnReplica(COUNT_POST_BY_TITLE, new Object[] {freshTitle}, EXPECTED_ONE_ROW, REPLICATION_TIMEOUT_MILLIS);
        }
    }

    @Test
    void 본인_방금쓴_글_상세는_마킹으로_primary라_보임() {
        String oldTitle = "마킹-old-title-" + System.nanoTime();
        String freshTitle = "마킹-fresh-title-" + System.nanoTime();

        Seed seed = seedPost(oldTitle);
        long postId = seed.postId;
        long writerId = seed.memberId;

        // old 제목이 replica 로 복제될 때까지 대기.
        awaitOnReplica(COUNT_POST_BY_TITLE, new Object[] {oldTitle}, EXPECTED_ONE_ROW, REPLICATION_TIMEOUT_MILLIS);

        // 복제를 얼린 뒤, 작성자 본인이 실제 서비스 쓰기(updatePost)로 제목을 fresh 로 변경.
        // 이 쓰기는 WriteMarkingAspect 포인트컷에 매칭되어 afterCommit 에 사용자를 마킹한다.
        stopReplication();
        try {
            MemberAuth writerAuth = new MemberAuth(writerId);
            UserContextHolder.set(writerId);
            postService.updatePost(postId, new PostEditRequest(freshTitle, "본문"), writerAuth);

            // 마킹된 작성자로 상세 조회 → recent-write 마킹으로 PRIMARY 강제 → fresh 제목을 본다.
            PostDetailResponse response = postService.getPostDetail(postId, writerAuth);

            assertThat(response.title())
                    .as("최근 쓰기 마킹이 있는 사용자의 read-only 조회는 PRIMARY 로 강제되어 fresh 제목을 본다")
                    .isEqualTo(freshTitle);
        } finally {
            startReplication();
            awaitOnReplica(COUNT_POST_BY_TITLE, new Object[] {freshTitle}, EXPECTED_ONE_ROW, REPLICATION_TIMEOUT_MILLIS);
        }
    }

    @Test
    void 조회수_증가_Redis_동작_유지() {
        String title = "조회수-title-" + System.nanoTime();
        Seed seed = seedPost(title);
        long postId = seed.postId;

        awaitOnReplica(COUNT_POST_BY_TITLE, new Object[] {title}, EXPECTED_ONE_ROW, REPLICATION_TIMEOUT_MILLIS);

        // read-only 로 재분류되더라도 Redis 기반 조회수 증가는 계속 동작해야 한다.
        UserContextHolder.clear();
        PostDetailResponse first = postService.getPostDetail(postId, null);
        PostDetailResponse second = postService.getPostDetail(postId, null);
        PostDetailResponse third = postService.getPostDetail(postId, null);

        assertThat(second.viewCount())
                .as("두 번째 조회의 조회수는 첫 번째보다 커야 한다(Redis 증가)")
                .isGreaterThan(first.viewCount());
        assertThat(third.viewCount())
                .as("반복 조회마다 조회수가 계속 증가해야 한다")
                .isGreaterThan(second.viewCount());
    }

    /**
     * 애플리케이션 라우팅 DataSource 를 통해 board+member+post 를 primary 에 심는다.
     * 쓰기 트랜잭션이므로 primary 로 라우팅되고, 커밋 후 binlog 로 replica 에 복제된다.
     */
    private Seed seedPost(String title) {
        AtomicLong postIdRef = new AtomicLong();
        AtomicLong memberIdRef = new AtomicLong();
        writeTx.executeWithoutResult(status -> {
            long nano = System.nanoTime();
            Member member = memberRepository.save(Member.builder()
                    .username("routing-user-" + nano)
                    .password("password1")
                    .nickname("routing-nick-" + nano)
                    .role(Role.USER)
                    .build());
            Board board = boardRepository.save(Board.builder()
                    .title("routing-board")
                    .description("라우팅 테스트 게시판")
                    .slug("routing-" + nano)
                    .boardType(BoardType.WRITABLE)
                    .build());
            Post post = postRepository.save(Post.builder()
                    .title(title)
                    .content("본문")
                    .member(member)
                    .board(board)
                    .build());
            postIdRef.set(post.getId());
            memberIdRef.set(member.getId());
        });
        return new Seed(postIdRef.get(), memberIdRef.get());
    }

    private record Seed(long postId, long memberId) {
    }
}
