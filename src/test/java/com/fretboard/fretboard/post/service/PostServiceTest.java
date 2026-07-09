package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.utils.AuthorizationHelper;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private AuthorizationHelper authorizationHelper;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .username("testuser")
                .password("password")
                .nickname("testnick")
                .role(Role.USER)
                .build();
    }

    @Test
    void addPost_성공() {
        // given
        Long memberId = 1L;
        Long boardId = 10L;
        Long savedPostId = 100L;

        MemberAuth memberAuth = new MemberAuth(memberId);
        PostNewRequest request = new PostNewRequest(boardId, "테스트 제목", "테스트 본문");

        Board board = Board.builder()
                .title("자유게시판")
                .description("자유롭게 작성하는 게시판")
                .slug("free")
                .boardType(BoardType.WRITABLE)
                .build();

        Post savedPost = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .member(member)
                .board(board)
                .build();
        ReflectionTestUtils.setField(savedPost, "id", savedPostId);

        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));
        given(imageService.convertTempImageUrlsToPermanent(anyString())).willReturn("테스트 본문");
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        // when
        Long resultId = postService.addPost(request, memberAuth);

        // then
        assertThat(resultId).isEqualTo(savedPostId);
    }

    @Test
    void addPost_게시판없으면_BOARD_NOT_FOUND_예외() {
        // given
        Long memberId = 1L;
        Long boardId = 999L;

        MemberAuth memberAuth = new MemberAuth(memberId);
        PostNewRequest request = new PostNewRequest(boardId, "테스트 제목", "테스트 본문");

        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(boardRepository.findById(boardId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.addPost(request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.BOARD_NOT_FOUND);
                });
    }

    @Test
    void addPost_쓰기불가게시판_예외() {
        // given
        Long memberId = 1L;
        Long boardId = 20L;

        MemberAuth memberAuth = new MemberAuth(memberId);
        PostNewRequest request = new PostNewRequest(boardId, "테스트 제목", "테스트 본문");

        Board board = Board.builder()
                .title("공지사항")
                .description("관리자 전용 게시판")
                .slug("notice")
                .boardType(BoardType.NON_WRITABLE)
                .build();

        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        given(boardRepository.findById(boardId)).willReturn(Optional.of(board));

        // when & then
        assertThatThrownBy(() -> postService.addPost(request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.FORBIDDEN_WRITE_PERMISSION);
                });
    }

    @Test
    void updatePost_성공() {
        // given
        Long postId = 100L;
        Long memberId = 1L;
        MemberAuth memberAuth = new MemberAuth(memberId);
        PostEditRequest request = new PostEditRequest("수정된 제목", "수정된 본문");

        Post post = Post.builder()
                .title("원래 제목")
                .content("원래 본문")
                .member(member)
                .board(null)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        willDoNothing().given(authorizationHelper).validateIsAuthor(any(), any());
        given(imageService.convertTempImageUrlsToPermanent(anyString())).willReturn("수정된 본문");

        // when
        postService.updatePost(postId, request, memberAuth);

        // then
        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 본문");
    }

    @Test
    void updatePost_게시글없으면_POST_NOT_FOUND_예외() {
        // given
        Long postId = 999L;
        Long memberId = 1L;
        MemberAuth memberAuth = new MemberAuth(memberId);
        PostEditRequest request = new PostEditRequest("수정된 제목", "수정된 본문");

        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.updatePost(postId, request, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.POST_NOT_FOUND);
                });
    }

    @Test
    void deletePost_성공() {
        // given
        Long postId = 100L;
        Long memberId = 1L;
        MemberAuth memberAuth = new MemberAuth(memberId);

        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .member(member)
                .board(null)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(member);
        willDoNothing().given(authorizationHelper).validateIsAuthor(any(), any());

        // when
        postService.deletePost(postId, memberAuth);

        // then
        verify(postRepository).delete(post);
        verify(viewCountService).deleteViewCount(postId);
    }

    @Test
    void deletePost_작성자불일치_NOT_AUTHOR_예외() {
        // given
        Long postId = 100L;
        Long memberId = 1L;
        MemberAuth memberAuth = new MemberAuth(memberId);

        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 본문")
                .member(member)
                .board(null)
                .build();

        Member otherMember = Member.builder()
                .username("otheruser")
                .password("password")
                .nickname("othernick")
                .role(Role.USER)
                .build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(authorizationHelper.getMember(memberAuth)).willReturn(otherMember);
        willThrow(new FretBoardException(ExceptionType.NOT_AUTHOR))
                .given(authorizationHelper).validateIsAuthor(any(), any());

        // when & then
        assertThatThrownBy(() -> postService.deletePost(postId, memberAuth))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> {
                    FretBoardException fbe = (FretBoardException) ex;
                    assertThat(fbe.getExceptionType()).isEqualTo(ExceptionType.NOT_AUTHOR);
                });
    }
}
