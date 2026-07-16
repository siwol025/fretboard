package com.fretboard.fretboard.post.service;

import com.fretboard.fretboard.comment.dto.PostCommentCountDto;
import com.fretboard.fretboard.comment.repository.CommentRepository;
import com.fretboard.fretboard.post.dto.PostSearchResultProjection;
import com.fretboard.fretboard.post.dto.PostSummaryDto;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.dto.response.PostSearchListResponse;
import com.fretboard.fretboard.post.dto.response.RecentPostsPerBoardResponse;
import com.fretboard.fretboard.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostFeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ViewCountService viewCountService;

    @InjectMocks
    private PostFeedService postFeedService;

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
        given(commentRepository.countCommentsByPostIds(List.of(10L)))
                .willReturn(List.of(new PostCommentCountDto(10L, 3L)));

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
        given(commentRepository.countCommentsByPostIds(List.of(10L)))
                .willReturn(List.of(new PostCommentCountDto(10L, 3L)));

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
        given(commentRepository.countCommentsByPostIds(List.of(10L)))
                .willReturn(List.of(new PostCommentCountDto(10L, 3L)));

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
        given(commentRepository.countCommentsByPostIds(List.of(20L)))
                .willReturn(List.of(new PostCommentCountDto(20L, 5L)));

        // when
        PostSearchListResponse result = postFeedService.searchPosts(boardId, keyword, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.posts()).hasSize(1);
        assertThat(result.posts().get(0).id()).isEqualTo(20L);
        assertThat(result.posts().get(0).commentCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getMostPosts — Redis 조회수 기반 인기글 Top5 반환")
    void getMostPosts_인기글_Top5_반환() {
        // given
        List<Long> topPostIds = List.of(1L, 2L);
        List<PostSummaryDto> topPostDtos = List.of(
                new PostSummaryDto(1L, "인기글1", "작성자A", LocalDateTime.now(), 100L),
                new PostSummaryDto(2L, "인기글2", "작성자B", LocalDateTime.now(), 80L)
        );

        given(viewCountService.getTopPosts(5)).willReturn(topPostIds);
        given(postRepository.findByPostIds(topPostIds)).willReturn(topPostDtos);

        // when
        List<PostSummaryDto> result = postFeedService.getMostPosts();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getRecentPosts — 게시판별 최근 게시글 목록 반환")
    void getRecentPosts_게시판별_최근게시글_반환() {
        // given
        given(postRepository.findRecentPostsPerBoards()).willReturn(List.of());

        // when
        List<RecentPostsPerBoardResponse> result = postFeedService.getRecentPosts();

        // then
        assertThat(result).isNotNull();
    }
}
