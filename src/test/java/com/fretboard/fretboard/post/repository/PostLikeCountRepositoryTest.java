package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PostLikeCountRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        entityManager.persist(member);

        Board board = Board.builder()
                .title("테스트 게시판")
                .description("테스트 게시판 설명")
                .slug("test-board")
                .boardType(BoardType.WRITABLE)
                .build();
        entityManager.persist(board);

        post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);

        entityManager.flush();
    }

    @Test
    void incrementLikeCount_는_like_count를_1_증가시킨다() {
        // given
        assertThat(post.getLikeCount()).isEqualTo(0L);

        // when
        postRepository.incrementLikeCount(post.getId());
        entityManager.clear();

        // then
        Post reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.getLikeCount()).isEqualTo(1L);
    }

    @Test
    void decrementLikeCount_는_like_count를_1_감소시킨다() {
        // given
        postRepository.incrementLikeCount(post.getId());
        postRepository.incrementLikeCount(post.getId());
        entityManager.clear();

        // when
        postRepository.decrementLikeCount(post.getId());
        entityManager.clear();

        // then
        Post reloaded = postRepository.findById(post.getId()).orElseThrow();
        assertThat(reloaded.getLikeCount()).isEqualTo(1L);
    }
}
