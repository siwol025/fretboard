package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.domain.PostLike;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PostLikeRepositoryTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

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
    void 좋아요_저장_및_조회() {
        // given
        PostLike postLike = PostLike.of(post, member);

        // when
        postLikeRepository.save(postLike);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<PostLike> found = postLikeRepository.findByPostIdAndMemberId(post.getId(), member.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPost().getId()).isEqualTo(post.getId());
        assertThat(found.get().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    void findByPostIdAndMemberId_없으면_empty_반환() {
        // when
        Optional<PostLike> found = postLikeRepository.findByPostIdAndMemberId(post.getId(), member.getId());

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void countByPostId_정상_집계() {
        // given
        Member member2 = Member.builder()
                .username("testuser2")
                .password("password")
                .nickname("testnick2")
                .role(Role.USER)
                .build();
        entityManager.persist(member2);

        postLikeRepository.save(PostLike.of(post, member));
        postLikeRepository.save(PostLike.of(post, member2));
        entityManager.flush();
        entityManager.clear();

        // when
        long count = postLikeRepository.countByPostId(post.getId());

        // then
        assertThat(count)
                .as("서로 다른 두 멤버의 좋아요가 모두 집계되어야 한다")
                .isEqualTo(2L);
    }

    @Test
    void uq_post_like_중복_좋아요_제약_위반() {
        // given
        postLikeRepository.save(PostLike.of(post, member));
        entityManager.flush();

        // when & then
        assertThatThrownBy(() -> {
            postLikeRepository.saveAndFlush(PostLike.of(post, member));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
