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
 * A/B 부하 테스트용 임시 클래스.
 * offset 방식 조회(findPostSummaryByBoardIdOffset)가 deferred join 방식과
 * 동일한 페이지에서 동일 결과(같은 게시글·같은 순서)를 반환하는지 검증한다.
 *
 * 주의: 이 테스트는 RED 상태(stub 이 빈 리스트를 반환)에서 의도적으로 실패한다.
 * 실제 offset 쿼리 구현 후 GREEN 으로 전환된다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PostOffsetPaginationRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member1;
    private Member member2;
    private Board board;

    @BeforeEach
    void setUp() {
        member1 = persistMember("user1", "nick1");
        member2 = persistMember("user2", "nick2");
        board = persistBoard("테스트 게시판", "test-board");
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
    void offset_방식이_deferred_방식과_동일_페이지에서_동일_결과를_반환한다() {
        // given — 같은 보드에 distinct createdAt 을 가진 게시글 5건
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        persistPost("post1", member1, board, base.plusMinutes(1));
        persistPost("post2", member2, board, base.plusMinutes(2));
        persistPost("post3", member1, board, base.plusMinutes(3));
        persistPost("post4", member2, board, base.plusMinutes(4));
        persistPost("post5", member1, board, base.plusMinutes(5));
        entityManager.clear();

        int size = 3;
        long offset = 1; // 2번째 페이지(offset=1): post4, post3, post2 순서

        // when — 두 방식 모두 동일한 boardId/size/offset 으로 조회
        List<PostSummaryDto> deferred = postRepository.findPostSummaryByBoardIdDeferred(board.getId(), size, offset);
        List<PostSummaryDto> plain = postRepository.findPostSummaryByBoardIdOffset(board.getId(), size, offset);

        // then — 두 결과가 완전히 동일해야 한다(같은 id, 같은 순서)
        assertThat(plain)
                .as("offset 방식은 deferred 방식과 같은 게시글 목록(동일 id, 동일 순서)을 반환해야 한다")
                .isEqualTo(deferred);
    }
}
