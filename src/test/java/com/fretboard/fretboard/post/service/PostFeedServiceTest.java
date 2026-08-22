package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.board.domain.Board;
import com.fretboard.fretboard.board.repository.BoardRepository;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.post.domain.Post;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.PostSearchListResponse;
import com.fretboard.fretboard.post.dto.response.PostSummaryResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.global.config.PopularProperties;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostFeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentCountLoader commentCountLoader;

    @Mock
    private ViewCountService viewCountService;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private PopularScoreService popularScoreService;

    private PostFeedService postFeedService;

    private static final PopularProperties.Weights WEIGHTS =
            new PopularProperties.Weights(1.0, 3.0, 2.0);

    private PostFeedService newService(int todayLimit) {
        PopularProperties props = new PopularProperties(WEIGHTS, 10, todayLimit, Duration.ofDays(2));
        return new PostFeedService(
                postRepository, boardRepository, commentCountLoader, popularScoreService, props);
    }

    @BeforeEach
    void setUp() {
        // 기존 테스트는 todayLimit=10 을 유지한다.
        postFeedService = newService(10);
    }

    @Test
    @DisplayName("getPostsByBoardId — 게시판 ID로 목록 페이징 반환")
    void getPostsByBoardId_게시판ID로_목록_페이징_반환() {
        // given
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PostSummaryDto postSummaryDto = new PostSummaryDto(
                10L, "테스트 제목", "작성자", LocalDateTime.now(), 5L
        );

        given(postRepository.findPostSummaryByBoardIdDeferred(eq(boardId), anyInt(), anyLong()))
                .willReturn(List.of(postSummaryDto));
        given(postRepository.countByBoardId(boardId)).willReturn(1L);
        given(commentCountLoader.load(List.of(10L)))
                .willReturn(Map.of(10L, 3L));

        // when
        PostListResponse result = postFeedService.getPostsByBoardId(boardId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).id()).isEqualTo(10L);
        assertThat(result.posts().get(0).commentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getPostsByBoardId — Deferred Join 조회 메서드가 호출된다")
    void getPostsByBoardId_Deferred_Join_조회_호출됨() {
        // given
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PostSummaryDto postSummaryDto = new PostSummaryDto(
                10L, "테스트 제목", "작성자", LocalDateTime.now(), 5L
        );

        given(postRepository.findPostSummaryByBoardIdDeferred(eq(boardId), anyInt(), anyLong()))
                .willReturn(List.of(postSummaryDto));
        given(postRepository.countByBoardId(boardId)).willReturn(1L);
        given(commentCountLoader.load(List.of(10L)))
                .willReturn(Map.of(10L, 3L));

        // when
        postFeedService.getPostsByBoardId(boardId, pageable);

        // then
        verify(postRepository).findPostSummaryByBoardIdDeferred(eq(boardId), anyInt(), anyLong());
    }

    @Test
    @DisplayName("getPostsByBoardId — buildCommentCountMap 재사용으로 commentCount 가 채워진다")
    void getPostsByBoardId_댓글수_포함_응답_구성() {
        // given
        Long boardId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PostSummaryDto postSummaryDto = new PostSummaryDto(
                10L, "테스트 제목", "작성자", LocalDateTime.now(), 5L
        );

        given(postRepository.findPostSummaryByBoardIdDeferred(eq(boardId), anyInt(), anyLong()))
                .willReturn(List.of(postSummaryDto));
        given(postRepository.countByBoardId(boardId)).willReturn(1L);
        given(commentCountLoader.load(List.of(10L)))
                .willReturn(Map.of(10L, 3L));

        // when
        PostListResponse result = postFeedService.getPostsByBoardId(boardId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).id()).isEqualTo(10L);
        assertThat(result.posts().get(0).commentCount()).isEqualTo(3L);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("searchPosts — 키워드 검색 결과 반환")
    void searchPosts_키워드로_검색결과_반환() {
        // given
        Long boardId = 1L;
        String keyword = "spring";
        Pageable pageable = PageRequest.of(0, 10);

        PostSearchResultProjection projection = mock(PostSearchResultProjection.class);
        given(projection.getId()).willReturn(20L);
        given(projection.getTitle()).willReturn("Spring 입문");
        given(projection.getAuthor()).willReturn("작성자");
        given(projection.getBoardId()).willReturn(boardId);
        given(projection.getBoardTitle()).willReturn("기술게시판");
        given(projection.getCreatedAt()).willReturn(LocalDateTime.now());
        given(projection.getViewCount()).willReturn(10L);

        Page<PostSearchResultProjection> projectionPage = new PageImpl<>(List.of(projection), pageable, 1);
        given(postRepository.searchByBoardIdAndKeyword(eq(boardId), eq(keyword), any(PageRequest.class)))
                .willReturn(projectionPage);
        given(commentCountLoader.load(List.of(20L)))
                .willReturn(Map.of(20L, 5L));

        // when
        PostSearchListResponse result = postFeedService.searchPosts(boardId, keyword, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).id()).isEqualTo(20L);
        assertThat(result.posts().get(0).commentCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getRecentPosts — 게시판별 최근 게시글 목록 반환")
    void getRecentPosts_게시판별_최근게시글_반환() {
        // given
        stubBoards(List.of());

        // when
        List<RecentPostsPerBoardResponse> result = postFeedService.getRecentPosts();

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getRecentPosts — 보드별 top5를 CommentCountLoader 로 채워 반환")
    void getRecentPosts_보드별_top5를_commentCountLoader로_채워_반환() {
        // given
        Board board1 = mockBoard(1L, "자유게시판", "free");
        Board board2 = mockBoard(2L, "질문게시판", "qna");
        stubBoards(List.of(board1, board2));

        Post post10 = mockPost(10L, "글10", board1, "닉A");
        Post post11 = mockPost(11L, "글11", board1, "닉A");
        Post post20 = mockPost(20L, "글20", board2, "닉B");

        given(postRepository.findTop5ByBoardIdOrderByRecency(1L))
                .willReturn(List.of(post10, post11));
        given(postRepository.findTop5ByBoardIdOrderByRecency(2L))
                .willReturn(List.of(post20));

        given(commentCountLoader.load(anyList()))
                .willReturn(Map.of(10L, 3L, 11L, 7L, 20L, 5L));

        // when
        List<RecentPostsPerBoardResponse> result = postFeedService.getRecentPosts();

        // then — commentCount 는 로더 맵에서 채워진다 (getComments().size() 아님)
        Map<Long, Integer> countById = result.stream()
                .flatMap(b -> b.getPosts().stream())
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.id(),
                        p -> p.commentCount()));
        assertThat(countById.get(10L)).isEqualTo(3);
        assertThat(countById.get(11L)).isEqualTo(7);
        assertThat(countById.get(20L)).isEqualTo(5);

        // N+1 회피: 카운트는 배치 로더로만, post.getComments() 는 호출되지 않는다
        verify(commentCountLoader).load(anyList());
        verify(post10, never()).getComments();
        verify(post11, never()).getComments();
        verify(post20, never()).getComments();
    }

    @Test
    @DisplayName("getRecentPosts — 글 없는 보드는 생략된다")
    void getRecentPosts_글없는_보드는_생략() {
        // given
        Board board1 = mockBoard(1L, "자유게시판", "free");
        Board board2 = mockBoard(2L, "질문게시판", "qna");
        stubBoards(List.of(board1, board2));

        Post post10 = mockPost(10L, "글10", board1, "닉A");

        given(postRepository.findTop5ByBoardIdOrderByRecency(1L))
                .willReturn(List.of(post10));
        given(postRepository.findTop5ByBoardIdOrderByRecency(2L))
                .willReturn(List.of());

        given(commentCountLoader.load(anyList()))
                .willReturn(Map.of(10L, 1L));

        // when
        List<RecentPostsPerBoardResponse> result = postFeedService.getRecentPosts();

        // then — 글 있는 보드(1L)만 포함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoardId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRecentPosts — 보드는 board_id 오름차순으로 반환된다")
    void getRecentPosts_보드는_board_id_오름차순() {
        // given
        Board board1 = mockBoard(1L, "자유게시판", "free");
        Board board2 = mockBoard(2L, "질문게시판", "qna");
        Board board3 = mockBoard(3L, "공지게시판", "notice");
        stubBoards(List.of(board1, board2, board3));

        Post post10 = mockPost(10L, "글10", board1, "닉A");
        Post post20 = mockPost(20L, "글20", board2, "닉B");
        Post post30 = mockPost(30L, "글30", board3, "닉C");

        given(postRepository.findTop5ByBoardIdOrderByRecency(1L))
                .willReturn(List.of(post10));
        given(postRepository.findTop5ByBoardIdOrderByRecency(2L))
                .willReturn(List.of(post20));
        given(postRepository.findTop5ByBoardIdOrderByRecency(3L))
                .willReturn(List.of(post30));

        given(commentCountLoader.load(anyList()))
                .willReturn(Map.of(10L, 0L, 20L, 0L, 30L, 0L));

        // when
        List<RecentPostsPerBoardResponse> result = postFeedService.getRecentPosts();

        // then
        assertThat(result).extracting(RecentPostsPerBoardResponse::getBoardId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("getTodayPopularPosts — 상위 10 점수 순서를 보존해 반환한다")
    void getTodayPopularPosts_상위10_점수순서_보존_반환() {
        // given — ZSet rank 순서: 3, 1, 2
        given(popularScoreService.getTopPostIds(10))
                .willReturn(List.of(3L, 1L, 2L));

        // findByPostIds 는 임의 순서(1, 2, 3)로 반환할 수 있다
        PostSummaryDto post1 = new PostSummaryDto(1L, "글1", "작성자1", LocalDateTime.now(), 5L);
        PostSummaryDto post2 = new PostSummaryDto(2L, "글2", "작성자2", LocalDateTime.now(), 7L);
        PostSummaryDto post3 = new PostSummaryDto(3L, "글3", "작성자3", LocalDateTime.now(), 9L);
        given(postRepository.findByPostIds(List.of(3L, 1L, 2L)))
                .willReturn(List.of(post1, post2, post3));

        // when
        List<PostSummaryDto> result = postFeedService.getTodayPopularPosts();

        // then — ZSet rank 순서([3, 1, 2])가 보존된다
        assertThat(result).extracting(PostSummaryDto::id)
                .containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("getTodayPopularPosts — 설정된 limit 으로 조회한다")
    void getTodayPopularPosts_설정된_limit_사용() {
        // given — 설정된 todayLimit 5
        postFeedService = newService(5);
        given(popularScoreService.getTopPostIds(5))
                .willReturn(List.of());

        // when
        postFeedService.getTodayPopularPosts();

        // then — 설정된 limit(5)로 조회되어야 한다
        verify(popularScoreService).getTopPostIds(5);
    }

    @Test
    @DisplayName("getTodayPopularPosts — 점수가 없으면 빈 리스트를 반환한다")
    void getTodayPopularPosts_점수없으면_빈리스트() {
        // given
        given(popularScoreService.getTopPostIds(10))
                .willReturn(List.of());

        // when
        List<PostSummaryDto> result = postFeedService.getTodayPopularPosts();

        // then
        assertThat(result).isEmpty();
    }

    // --- fixtures ---

    private void stubBoards(List<Board> boards) {
        // GREEN 이 findAll() 또는 findAll(Sort) 중 무엇을 쓰든 동작하도록 둘 다 lenient 스텁
        lenient().when(boardRepository.findAll()).thenReturn(boards);
        lenient().when(boardRepository.findAll(any(Sort.class))).thenReturn(boards);
    }

    private Board mockBoard(Long id, String title, String slug) {
        Board board = mock(Board.class);
        lenient().when(board.getId()).thenReturn(id);
        lenient().when(board.getTitle()).thenReturn(title);
        lenient().when(board.getSlug()).thenReturn(slug);
        return board;
    }

    private Post mockPost(Long id, String title, Board board, String nickname) {
        Post post = mock(Post.class);
        lenient().when(post.getId()).thenReturn(id);
        lenient().when(post.getTitle()).thenReturn(title);
        lenient().when(post.getViewCount()).thenReturn(0L);
        lenient().when(post.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(post.getBoard()).thenReturn(board);
        Member member = mock(Member.class);
        lenient().when(member.getNickname()).thenReturn(nickname);
        lenient().when(post.getMember()).thenReturn(member);
        return post;
    }
}
