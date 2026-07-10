package com.fretboard.fretboard.comment.repository;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import com.fretboard.fretboard.global.config.JpaAuditingConfig;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void countCommentsByPostIds_삭제된_댓글은_카운트에서_제외됨() {
        // given
        Member member = Member.builder()
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

        Post post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(board)
                .build();
        entityManager.persist(post);

        Comment activeComment1 = Comment.parent("활성 댓글 1", member, post);
        entityManager.persist(activeComment1);

        Comment activeComment2 = Comment.parent("활성 댓글 2", member, post);
        entityManager.persist(activeComment2);

        Comment deletedComment = Comment.parent("삭제된 댓글", member, post);
        entityManager.persist(deletedComment);
        deletedComment.softDelete();

        entityManager.flush();
        entityManager.clear();

        // when
        List<PostCommentCountDto> result = commentRepository.countCommentsByPostIds(List.of(post.getId()));

        // then
        // 삭제된 댓글(1개)이 카운트에서 제외되어 활성 댓글 2개만 집계되어야 한다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).commentCount())
                .as("삭제된 댓글(1개)은 카운트에서 제외되고 활성 댓글(2개)만 집계되어야 한다")
                .isEqualTo(2L);
    }
}
