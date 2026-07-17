package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 내 게시글 목록 Deferred Join native query 를 H2(MODE=MYSQL) 에서 검증한다.
 * 여러 게시판에 걸친 게시글에 대해 member 필터·최신순 정렬·board 정보 포함·타이브레이커를 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MyPostDeferredJoinRepositoryTest {

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
        // auditing 으로 세팅된 createdAt 을 강제로 덮어써서 정렬 시나리오를 통제한다.
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        entityManager.merge(post);
        entityManager.flush();
        return post;
    }

    @Test
    void 내_게시글만_필터링되어_최신순_반환() {
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

        // then — member1 것만, created_at DESC
        assertThat(result)
                .as("내 게시글만 필터링되어 created_at DESC 로 반환되어야 한다")
                .hasSize(3)
                .extracting(MyPostSummaryDto::title)
                .containsExactly("m1-post3", "m1-post2", "m1-post1");
    }

    @Test
    void board_정보가_포함되어_반환() {
        // given — member1 게시글이 서로 다른 board 에 걸쳐 있음
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("m1-board1-post", member1, board1, base.plusMinutes(1));
        persistPost("m1-board2-post", member1, board2, base.plusMinutes(2));
        entityManager.clear();

        // when
        List<MyPostSummaryDto> result = postRepository.findMyPostSummaryDeferred(member1.getId(), 10, 0);

        // then — 각 게시글의 boardId/boardTitle 이 실제 board 와 일치해야 한다
        assertThat(result)
                .as("board 정보(boardId/boardTitle)가 각 게시글의 board 와 일치해야 한다")
                .extracting(MyPostSummaryDto::title, MyPostSummaryDto::boardId, MyPostSummaryDto::boardTitle)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("m1-board2-post", board2.getId(), board2.getTitle()),
                        org.assertj.core.groups.Tuple.tuple("m1-board1-post", board1.getId(), board1.getTitle())
                );
    }

    @Test
    void 동일_createdAt_id_타이브레이커_중복_누락_없음() {
        // given — member1 의 동일 createdAt 4건 (id 만 다름)
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 12, 0);
        Post p1 = persistPost("same1", member1, board1, sameTime);
        Post p2 = persistPost("same2", member1, board1, sameTime);
        Post p3 = persistPost("same3", member1, board2, sameTime);
        Post p4 = persistPost("same4", member1, board2, sameTime);
        entityManager.clear();

        // when — 2건씩 두 페이지
        List<MyPostSummaryDto> firstPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 0);
        List<MyPostSummaryDto> secondPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 2);

        // then — id DESC 타이브레이커로 중복/누락 없이 순서대로
        assertThat(firstPage)
                .extracting(MyPostSummaryDto::id)
                .as("첫 페이지는 id 내림차순 상위 2건")
                .containsExactly(p4.getId(), p3.getId());

        assertThat(secondPage)
                .extracting(MyPostSummaryDto::id)
                .as("두 번째 페이지는 id 내림차순 하위 2건 — 중복/누락 없음")
                .containsExactly(p2.getId(), p1.getId());
    }

    @Test
    void 내_게시글이_없으면_빈_리스트_반환() {
        // given — member1 은 게시글이 0건 (member2 만 게시글 보유)
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("m2-post1", member2, board1, base.plusMinutes(1));
        entityManager.clear();

        // when
        List<MyPostSummaryDto> result = postRepository.findMyPostSummaryDeferred(member1.getId(), 10, 0);

        // then
        assertThat(result)
                .as("게시글이 없는 member 는 빈 리스트를 반환해야 한다")
                .isEmpty();
    }

    @Test
    void offset이_총건수보다_크면_빈_리스트_반환() {
        // given — member1 게시글 3건
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("m1-post1", member1, board1, base.plusMinutes(1));
        persistPost("m1-post2", member1, board2, base.plusMinutes(2));
        persistPost("m1-post3", member1, board1, base.plusMinutes(3));
        entityManager.clear();

        // when — offset 이 총건수(3)보다 훨씬 큼
        List<MyPostSummaryDto> result = postRepository.findMyPostSummaryDeferred(member1.getId(), 10, 13);

        // then
        assertThat(result)
                .as("offset 이 총건수보다 크면 빈 리스트를 반환해야 한다")
                .isEmpty();
    }

    @Test
    void 정확히_size_배수_경계_정상_반환() {
        // given — member1 게시글 4건, size=2 로 페이징
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("m1-post1", member1, board1, base.plusMinutes(1));
        persistPost("m1-post2", member1, board2, base.plusMinutes(2));
        persistPost("m1-post3", member1, board1, base.plusMinutes(3));
        persistPost("m1-post4", member1, board2, base.plusMinutes(4));
        entityManager.clear();

        // when — 2건씩 세 페이지 요청 (마지막은 경계 밖)
        List<MyPostSummaryDto> firstPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 0);
        List<MyPostSummaryDto> secondPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 2);
        List<MyPostSummaryDto> thirdPage = postRepository.findMyPostSummaryDeferred(member1.getId(), 2, 4);

        // then — 두 페이지가 각각 2건, 세 번째 페이지(경계 밖)는 빈 리스트
        assertThat(firstPage)
                .as("첫 페이지는 최신순 상위 2건")
                .extracting(MyPostSummaryDto::title)
                .containsExactly("m1-post4", "m1-post3");
        assertThat(secondPage)
                .as("두 번째 페이지는 최신순 하위 2건")
                .extracting(MyPostSummaryDto::title)
                .containsExactly("m1-post2", "m1-post1");
        assertThat(thirdPage)
                .as("size 배수 경계(offset=총건수)의 다음 페이지는 빈 리스트")
                .isEmpty();
    }
}
