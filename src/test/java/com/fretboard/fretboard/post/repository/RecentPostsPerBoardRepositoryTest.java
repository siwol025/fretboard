package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class RecentPostsPerBoardRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Board targetBoard;
    private Board otherBoard;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        entityManager.persist(member);

        targetBoard = Board.builder()
                .title("대상 게시판")
                .description("대상 게시판 설명")
                .slug("target-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(targetBoard);

        otherBoard = Board.builder()
                .title("다른 게시판")
                .description("다른 게시판 설명")
                .slug("other-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(otherBoard);
    }

    private Post persistPost(String title, Board board, LocalDateTime createdAt) {
        Post post = Post.builder()
                .title(title)
                .content("본문 " + title)
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);
        entityManager.flush();
        // auditing 으로 세팅된 createdAt 을 강제로 덮어써서 정렬 시나리오를 통제한다.
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        entityManager.merge(post);
        entityManager.flush();
        return post;
    }

    @Test
    @DisplayName("특정 boardId 의 최신 게시글을 created_at DESC, id DESC 로 최대 5개 반환한다")
    void 특정_게시판의_최신_게시글_top5를_최신순_id내림차순으로_반환한다() {
        // given — 대상 게시판에 6건 이상 저장. 동일 createdAt 은 id DESC 로 정렬되어야 한다.
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("t-oldest", targetBoard, base.plusMinutes(1));  // 6번째 최신 → 제외 대상
        Post tie1 = persistPost("t-tie-a", targetBoard, base.plusMinutes(2)); // 동률(낮은 id)
        Post tie2 = persistPost("t-tie-b", targetBoard, base.plusMinutes(2)); // 동률(높은 id)
        Post t3 = persistPost("t-3", targetBoard, base.plusMinutes(3));
        Post t4 = persistPost("t-4", targetBoard, base.plusMinutes(4));
        Post t5 = persistPost("t-5", targetBoard, base.plusMinutes(5));

        // 다른 게시판 글 2건 — boardId 필터링 검증용
        persistPost("other-1", otherBoard, base.plusMinutes(10));
        persistPost("other-2", otherBoard, base.plusMinutes(11));

        entityManager.clear();

        // when
        List<Post> result = postRepository.findTop5ByBoardIdOrderByRecency(targetBoard.getId());

        // then — 정확히 5건, 모두 대상 게시판, created_at DESC → id DESC 순서
        assertThat(result)
                .as("정확히 5건이 반환되어야 한다")
                .hasSize(5);

        assertThat(result)
                .as("모두 대상 게시판의 글이어야 한다")
                .allSatisfy(p -> assertThat(p.getBoard().getId()).isEqualTo(targetBoard.getId()));

        assertThat(result)
                .as("created_at DESC, 동률이면 id DESC 순서여야 하며 가장 오래된 글은 제외된다")
                .extracting(Post::getId)
                .containsExactly(t5.getId(), t4.getId(), t3.getId(), tie2.getId(), tie1.getId());
    }
}
