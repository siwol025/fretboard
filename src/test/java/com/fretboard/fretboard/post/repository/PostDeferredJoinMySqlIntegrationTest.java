package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.global.support.AbstractMySqlContainerTest;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 MySQL 8.4(Testcontainers)에 대해 Deferred Join native query 와 Flyway 마이그레이션을 검증한다.
 * H2(MODE=MYSQL) 테스트가 놓치는 MySQL 전용 비호환(예: WHERE id IN (...LIMIT) → ERROR 1235)을
 * 실제 엔진에서 잡는 것이 목적이다.
 *
 * <p>컨테이너 기동 및 datasource 배선은 {@link AbstractMySqlContainerTest} 에서 처리한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostDeferredJoinMySqlIntegrationTest extends AbstractMySqlContainerTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Board board;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        entityManager.persist(member);

        board = Board.builder()
                .title("테스트 게시판")
                .description("테스트 게시판 설명")
                .slug("test-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(board);
    }

    private Post persistPost(String title, LocalDateTime createdAt) {
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
    void 실제_MySQL에서_Deferred_Join_정렬_정확성() {
        // given — created_at 이 서로 다른 5건
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("post1", base.plusMinutes(1));
        persistPost("post2", base.plusMinutes(2));
        persistPost("post3", base.plusMinutes(3));
        persistPost("post4", base.plusMinutes(4));
        persistPost("post5", base.plusMinutes(5));
        entityManager.clear();

        // when — offset=0, size=3
        List<PostSummaryDto> result = postRepository.findPostSummaryByBoardIdDeferred(board.getId(), 3, 0);

        // then — created_at DESC 로 최신 3건
        assertThat(result)
                .as("실제 MySQL 에서 created_at DESC 로 size 개를 반환해야 한다")
                .hasSize(3)
                .extracting(PostSummaryDto::title)
                .containsExactly("post5", "post4", "post3");
    }

    @Test
    void 실제_MySQL에서_동일_createdAt_id_타이브레이커_페이지_경계() {
        // given — 동일 created_at 4건 (id 만 다름)
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        Post p1 = persistPost("same1", sameTime);
        Post p2 = persistPost("same2", sameTime);
        Post p3 = persistPost("same3", sameTime);
        Post p4 = persistPost("same4", sameTime);
        entityManager.clear();

        // when — 2건씩 두 페이지
        List<PostSummaryDto> firstPage = postRepository.findPostSummaryByBoardIdDeferred(board.getId(), 2, 0);
        List<PostSummaryDto> secondPage = postRepository.findPostSummaryByBoardIdDeferred(board.getId(), 2, 2);

        // then — id DESC 타이브레이커로 중복/누락 없음
        assertThat(firstPage)
                .extracting(PostSummaryDto::id)
                .as("첫 페이지는 id 내림차순 상위 2건")
                .containsExactly(p4.getId(), p3.getId());

        assertThat(secondPage)
                .extracting(PostSummaryDto::id)
                .as("두 번째 페이지는 id 내림차순 하위 2건 — 중복/누락 없음")
                .containsExactly(p2.getId(), p1.getId());
    }
}
