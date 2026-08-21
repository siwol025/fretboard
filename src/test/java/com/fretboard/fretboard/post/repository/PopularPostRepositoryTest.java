package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * 게시판별 인기글(승격글) 조회 리포지토리 통합 테스트.
 * popular_at IS NOT NULL 인 글만, popular_at DESC 로 정렬해 페이징 반환하는지 검증한다.
 *
 * RED 단계: findPopularPostSummaryByBoardId 는 아직 임시 스텁(@Query WHERE 1=0)이라 빈 결과를 반환하므로
 * 아래 테스트는 결과 단언에서 실패한다(컨텍스트 로드 실패가 아님).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PopularPostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Board boardA;
    private Board boardB;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        entityManager.persist(member);

        boardA = Board.builder()
                .title("게시판 A")
                .description("게시판 A 설명")
                .slug("board-a")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(boardA);

        boardB = Board.builder()
                .title("게시판 B")
                .description("게시판 B 설명")
                .slug("board-b")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(boardB);
    }

    private Post persistPost(String title, Board board) {
        Post post = Post.builder()
                .title(title)
                .content("본문 " + title)
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);
        entityManager.flush();
        return post;
    }

    /**
     * 게시글을 저장한 뒤 promote() 로 승격하고, popular_at 값을 명시적으로 통제해 flush 한다.
     * (promote() 는 popular_at 을 now() 로 세팅하므로, 정렬 시나리오 통제를 위해 값을 덮어쓴다.)
     */
    private Post persistPromotedPost(String title, Board board, LocalDateTime popularAt) {
        Post post = persistPost(title, board);
        post.promote();
        entityManager.flush();
        ReflectionTestUtils.setField(post, "popularAt", popularAt);
        entityManager.merge(post);
        entityManager.flush();
        return post;
    }

    @Test
    void 게시판별_승격글만_조회() {
        // given — 게시판 A: 승격글 2건 + 비승격글 1건, 게시판 B: 승격글 1건
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        Post a1 = persistPromotedPost("A-인기글1", boardA, base.plusMinutes(1));
        Post a2 = persistPromotedPost("A-인기글2", boardA, base.plusMinutes(2));
        persistPost("A-일반글", boardA);
        persistPromotedPost("B-인기글", boardB, base.plusMinutes(3));
        entityManager.clear();

        // when — 게시판 A 인기글 조회
        List<PostSummaryDto> result =
                postRepository.findPopularPostSummaryByBoardId(boardA.getId(), 10, 0);

        // then — 게시판 A 의 승격글 2건만 (비승격글·게시판 B 승격글 제외)
        assertThat(result)
                .as("게시판 A 의 승격글만 조회되어야 한다 (비승격글·타 게시판 제외)")
                .extracting(PostSummaryDto::id)
                .containsExactlyInAnyOrder(a1.getId(), a2.getId());
    }

    @Test
    void popular_at_내림차순_정렬() {
        // given — popular_at 이 서로 다른 승격글 3건 (게시판 A)
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPromotedPost("오래된-인기글", boardA, base.plusMinutes(1));
        persistPromotedPost("중간-인기글", boardA, base.plusMinutes(2));
        persistPromotedPost("최신-인기글", boardA, base.plusMinutes(3));
        entityManager.clear();

        // when
        List<PostSummaryDto> result =
                postRepository.findPopularPostSummaryByBoardId(boardA.getId(), 10, 0);

        // then — popular_at DESC (최신 승격이 먼저)
        assertThat(result)
                .as("게시판 A 인기글은 popular_at 내림차순으로 정렬되어야 한다")
                .extracting(PostSummaryDto::title)
                .containsExactly("최신-인기글", "중간-인기글", "오래된-인기글");
    }
}
