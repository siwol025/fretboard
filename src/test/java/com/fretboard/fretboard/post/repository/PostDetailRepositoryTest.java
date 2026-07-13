package com.fretboard.fretboard.post.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class PostDetailRepositoryTest {

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

        // 댓글 작성자는 게시글 작성자(member)와 다른 회원으로 지정한다.
        // 동일 회원이면 p.member JOIN FETCH 로 이미 로딩되어 comment.member 미fetch 결함이 드러나지 않기 때문.
        Member commentAuthor = Member.builder()
                .username("commentuser")
                .password("password")
                .nickname("commentnick")
                .role(Role.USER)
                .build();
        entityManager.persist(commentAuthor);

        Comment comment = Comment.builder()
                .content("테스트 댓글")
                .post(post)
                .member(commentAuthor)
                .build();
        entityManager.persist(comment);

        entityManager.flush();
    }

    @Test
    void findPostDetailById_호출_시_member_board_comments_가_즉시_로딩된다() {
        // when — 영속성 컨텍스트를 비워 lazy 프록시 접근이 불가능한 상태에서 검증
        entityManager.clear();
        Optional<Post> result = postRepository.findPostDetailById(post.getId());

        // then
        assertThat(result).isPresent();
        Post post = result.get();

        assertThat(Hibernate.isInitialized(post.getMember()))
                .as("member 가 JOIN FETCH 로 즉시 로딩되어야 한다")
                .isTrue();

        assertThat(Hibernate.isInitialized(post.getBoard()))
                .as("board 가 JOIN FETCH 로 즉시 로딩되어야 한다")
                .isTrue();

        assertThat(Hibernate.isInitialized(post.getComments()))
                .as("comments 가 LEFT JOIN FETCH 로 즉시 로딩되어야 한다")
                .isTrue();
    }

    @Test
    void findPostDetailById_호출_시_각_댓글의_작성자도_즉시_로딩된다() {
        // when — 영속성 컨텍스트를 비워 lazy 프록시 접근이 불가능한 상태에서 검증
        entityManager.clear();
        Optional<Post> result = postRepository.findPostDetailById(post.getId());

        // then
        assertThat(result).isPresent();
        Post post = result.get();

        Comment comment = post.getComments().get(0);
        assertThat(Hibernate.isInitialized(comment.getMember()))
                .as("각 댓글의 작성자(member) 가 JOIN FETCH 로 즉시 로딩되어 N+1 이 발생하지 않아야 한다")
                .isTrue();
    }
}
