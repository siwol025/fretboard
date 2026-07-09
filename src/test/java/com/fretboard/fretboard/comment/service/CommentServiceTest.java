package com.fretboard.fretboard.comment.service;

import com.fretboard.fretboard.comment.domain.Comment;
import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.helper.AuthorizationHelper;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AuthorizationHelper authorizationHelper;

    private Member member;
    private MemberAuth memberAuth;
    private Post post;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
        memberAuth = new MemberAuth(1L);
        post = Post.builder()
                .title("테스트 게시글")
                .content("게시글 본문")
                .member(member)
                .board(null)
                .build();
    }

    @Test
    void addComment_성공() {
        // given
        Long postId = 10L;
        CommentRequest request = new CommentRequest("테스트 댓글 내용");

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        commentService.addComment(postId, request, memberAuth);

        // then
        verify(commentRepository).save(any(Comment.class));
        assertThat(post.getComments()).hasSize(1);
        assertThat(post.getComments().get(0).getContent()).isEqualTo("테스트 댓글 내용");
    }

    @Test
    void addComment_게시글없으면_POST_NOT_FOUND_예외() {
        // given
        Long postId = 999L;
        CommentRequest request = new CommentRequest("테스트 댓글 내용");

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.addComment(postId, request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.POST_NOT_FOUND);
                });
    }

    @Test
    void editComment_성공() {
        // given
        Long commentId = 20L;
        CommentRequest request = new CommentRequest("수정된 댓글 내용");

        Comment comment = Comment.parent("원래 댓글 내용", member, post);

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);

        // when
        commentService.editComment(commentId, request, memberAuth);

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 댓글 내용");
    }

    @Test
    void findComments_Member를_JOIN_FETCH로_함께_조회() {
        // given
        Long postId = 10L;
        Comment comment = Comment.parent("댓글 내용", member, post);
        given(commentRepository.findCommentsByPostIdWithMember(postId)).willReturn(List.of(comment));

        // when
        commentService.findComments(postId);

        // then
        verify(commentRepository).findCommentsByPostIdWithMember(postId);
    }

    @Test
    void addComment_XSS_script태그_제거후_저장() {
        // given
        Long postId = 10L;
        CommentRequest request = new CommentRequest("<script>alert('xss')</script>텍스트");

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        commentService.addComment(postId, request, memberAuth);

        // then
        assertThat(post.getComments()).hasSize(1);
        assertThat(post.getComments().get(0).getContent()).doesNotContain("<script>");
        assertThat(post.getComments().get(0).getContent()).contains("텍스트");
    }

    @Test
    void editComment_XSS_script태그_제거후_저장() {
        // given
        Long commentId = 20L;
        CommentRequest request = new CommentRequest("<script>alert('xss')</script>수정내용");

        Comment comment = Comment.parent("원래 댓글 내용", member, post);

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);

        // when
        commentService.editComment(commentId, request, memberAuth);

        // then
        assertThat(comment.getContent()).doesNotContain("<script>");
        assertThat(comment.getContent()).contains("수정내용");
    }

    @Test
    void deleteComment_작성자불일치_NOT_AUTHOR_예외() {
        // given
        Long commentId = 30L;

        Member otherMember = Member.builder()
                .username("otheruser")
                .password("password")
                .nickname("othernick")
                .role(Role.USER)
                .build();

        Comment comment = Comment.parent("댓글 내용", member, post);

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(authorizationHelper.getMember(memberAuth)).willReturn(otherMember);
        willThrow(new FretBoardException(ExceptionType.NOT_AUTHOR))
                .given(authorizationHelper).validateIsAuthor(any(), any());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(commentId, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.NOT_AUTHOR);
                });
    }
}
