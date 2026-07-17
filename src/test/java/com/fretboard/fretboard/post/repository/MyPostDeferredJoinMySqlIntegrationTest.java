package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.global.support.AbstractMySqlContainerTest;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.MyPostSummaryDto;
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
import static org.assertj.core.api.Assertions.tuple;

/**
 * 내 게시글 목록 Deferred Join native query 를 실제 MySQL 8.4(Testcontainers)에서 검증한다.
 * H2 가 놓치는 MySQL 전용 비호환(예: IN(...LIMIT) → ERROR 1235)을 실제 엔진에서 잡는 것이 목적이다.
 * 컨테이너 기동 및 datasource 배선은 {@link AbstractMySqlContainerTest} 에서 처리한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class MyPostDeferredJoinMySqlIntegrationTest extends AbstractMySqlContainerTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member1;
    private Member member2;
    private Board board1;
    private Board board2;

    @BeforeEach
    void setUp() {
        member1 = persistMember("user1", "nick1");
        member2 = persistMember("user2", "nick2");
        board1 = persistBoard("게시판1", "board-1");
        board2 = persistBoard("게시판2", "board-2");
    }

    private Member persistMember(String username, String nickname) {
        Member member = Member.builder()
                .username(username)
                .password("password")
                .nickname(nickname)
                .role(Role.USER)
                .build();
        entityManager.persist(member);
        return member;
    }

    private Board persistBoard(String title, String slug) {
        Board board = Board.builder()
                .title(title)
                .description(title + " 설명")
                .slug(slug)
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(board);
        return board;
    }

    private Post persistPost(String title, Member member, Board board, LocalDateTime createdAt) {
        Post post = Post.builder()
                .title(title)
                .content("본문 " + title)
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);
        entityManager.flush();
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        entityManager.merge(post);
        entityManager.flush();
        return post;
    }

    @Test
    void 실제_MySQL에서_내_게시글_필터_및_board정보_최신순() {
        // given — member1 게시글 3건(서로 다른 board), member2 게시글 2건
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("m1-post1", member1, board1, base.plusMinutes(1));
        persistPost("m1-post2", member1, board2, base.plusMinutes(2));
        persistPost("m1-post3", member1, board1, base.plusMinutes(3));
        persistPost("m2-post1", member2, board1, base.plusMinutes(4));
        persistPost("m2-post2", member2, board2, base.plusMinutes(5));
        entityManager.clear();

        // when
        List<MyPostSummaryDto> result = postRepository.findMyPostSummaryDeferred(member1.getId(), 10, 0);

        // then — member1 것만, created_at DESC, board 정보 포함
        assertThat(result)
                .as("실제 MySQL 에서 내 게시글만 created_at DESC 로 board 정보와 함께 반환되어야 한다")
                .hasSize(3)
                .extracting(MyPostSummaryDto::title, MyPostSummaryDto::boardId, MyPostSummaryDto::boardTitle)
                .containsExactly(
                        tuple("m1-post3", board1.getId(), board1.getTitle()),
                        tuple("m1-post2", board2.getId(), board2.getTitle()),
                        tuple("m1-post1", board1.getId(), board1.getTitle())
                );
    }

    @Test
    void 실제_MySQL에서_동일_createdAt_id_타이브레이커_페이지_경계() {
        // given — member1 동일 created_at 4건
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        Post p1 = persistPost("same1", member1, board1, sameTime);
        Post p2 = persistPost("same2", member1, board1, sameTime);
        Post p3 = persistPost("same3", member1, board2, sameTime);
        Post p4 = persistPost("same4", member1, board2, sameTime);
        entityManager.clear();

        // when — 2건씩 두 페이지
        List<MyPostSummaryDto> firstPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 0);
        List<MyPostSummaryDto> secondPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 2);

        // then — id DESC 타이브레이커로 중복/누락 없음
        assertThat(firstPage)
                .extracting(MyPostSummaryDto::id)
                .as("첫 페이지는 id 내림차순 상위 2건")
                .containsExactly(p4.getId(), p3.getId());

        assertThat(secondPage)
                .extracting(MyPostSummaryDto::id)
                .as("두 번째 페이지는 id 내림차순 하위 2건 — 중복/누락 없음")
                .containsExactly(p2.getId(), p1.getId());
    }
}
