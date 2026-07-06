package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.global.auth.dto.MemberAuth;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.global.helper.AuthorizationHelper;
import com.fretboard.fretboard.image.service.ImageService;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSearchSummaryDto;
import com.fretboard.fretboard.post.dto.request.PostEditRequest;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.PostSearchListResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    private CommentRepository commentRepository;

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

    private PostSearchResultProjection mockSearchProjection(Long boardId) {
        PostSearchResultProjection proj = mock(PostSearchResultProjection.class);
        given(proj.getId()).willReturn(1L);
        given(proj.getTitle()).willReturn("기타 입문");
        given(proj.getAuthor()).willReturn("testnick");
        given(proj.getBoardId()).willReturn(boardId);
        given(proj.getBoardTitle()).willReturn("자유게시판");
        given(proj.getCreatedAt()).willReturn(LocalDateTime.now());
        given(proj.getViewCount()).willReturn(0L);
        return proj;
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
        savedPost.setId(savedPostId);

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

        // NON_WRITABLE 게시판 - 일반 USER는 쓰기 불가 (관리자만 가능)
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
    void searchPosts_키워드_검색시_commentRepository로_댓글수_조회() {
        // given
        Long boardId = 10L;
        String keyword = "기타";
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResultProjection proj = mockSearchProjection(boardId);
        Page<PostSearchResultProjection> resultPage = new PageImpl<>(List.of(proj), pageable, 1);

        given(postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable))
                .willReturn(resultPage);
        given(commentRepository.countCommentsByPostIds(anyList()))
                .willReturn(List.of());

        // when
        postService.searchPosts(boardId, keyword, pageable);

        // then
        verify(commentRepository).countCommentsByPostIds(List.of(1L));
    }

    @Test
    void searchPosts_결과_없으면_빈_페이지_반환() {
        // given
        Long boardId = 10L;
        String keyword = "없는키워드";
        Pageable pageable = PageRequest.of(0, 10);

        Page<PostSearchResultProjection> emptyPage = Page.empty(pageable);
        given(postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable))
                .willReturn(emptyPage);
        given(commentRepository.countCommentsByPostIds(List.of()))
                .willReturn(List.of());

        // when
        PostSearchListResponse response = postService.searchPosts(boardId, keyword, pageable);

        // then
        assertThat(response.totalElements()).isEqualTo(0L);
        assertThat(response.posts()).isEmpty();
    }

    @Test
    void searchPosts_결과에_boardId_boardTitle_commentCount_포함() {
        // given
        Long boardId = 10L;
        String boardTitle = "자유게시판";
        String keyword = "기타";
        Long postId = 1L;
        Long expectedCommentCount = 3L;
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResultProjection proj = mockSearchProjection(boardId);
        Page<PostSearchResultProjection> resultPage = new PageImpl<>(List.of(proj), pageable, 1);

        given(postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable))
                .willReturn(resultPage);
        given(commentRepository.countCommentsByPostIds(List.of(postId)))
                .willReturn(List.of(new com.fretboard.fretboard.comment.dto.PostCommentCountDto(postId, expectedCommentCount)));

        // when
        PostSearchListResponse response = postService.searchPosts(boardId, keyword, pageable);

        // then
        assertThat(response.posts().get(0)).isInstanceOf(PostSearchSummaryDto.class);
        PostSearchSummaryDto first = response.posts().get(0);
        assertThat(first.boardId()).isEqualTo(boardId);
        assertThat(first.boardTitle()).isEqualTo(boardTitle);
        assertThat(first.commentCount()).isEqualTo(expectedCommentCount);
    }

    @Test
    void searchPosts_FULLTEXT_쿼리_호출시_boardId_keyword_전달() {
        // given
        Long boardId = 10L;
        String keyword = "기타";
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResultProjection proj = mockSearchProjection(boardId);
        Page<PostSearchResultProjection> projPage = new PageImpl<>(List.of(proj), pageable, 1);
        given(postRepository.searchByBoardIdAndKeyword(boardId, keyword, pageable))
                .willReturn(projPage);
        given(commentRepository.countCommentsByPostIds(anyList())).willReturn(List.of());

        // when
        postService.searchPosts(boardId, keyword, pageable);

        // then
        verify(postRepository).searchByBoardIdAndKeyword(boardId, keyword, pageable);
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
