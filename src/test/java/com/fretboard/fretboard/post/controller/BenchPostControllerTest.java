package com.fretboard.fretboard.post.controller;

import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.datasource.UserContextFilter;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.post.dto.PostWithCommentCountDto;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.service.PostFeedService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BenchPostController 슬라이스 테스트.
 * GET /api/_bench/posts?boardId=..&page=..&size=.. 가 offset 방식으로 PostListResponse를 반환함을 검증.
 */
@WebMvcTest(
        controllers = BenchPostController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class, UserContextFilter.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class BenchPostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PostFeedService postFeedService;

    @Test
    @DisplayName("GET /api/_bench/posts?boardId=1 은 offset 방식으로 조회한 게시글 목록(PostListResponse)을 반환한다")
    void getPostsByBoardIdOffset_boardId1_offset방식_PostListResponse_반환() throws Exception {
        // given — offset 방식 서비스가 한 건짜리 목록을 반환하도록 스텁
        PostWithCommentCountDto post = new PostWithCommentCountDto(
                1L, "벤치 제목", "작성자", LocalDateTime.now(), 0L, 0L);
        PostListResponse response = new PostListResponse(
                List.of(post), 0, 1, 1L, true, true);
        given(postFeedService.getPostsByBoardIdOffset(eq(1L), any(Pageable.class)))
                .willReturn(response);

        // when & then — 컨트롤러가 offset 서비스 결과를 그대로 직렬화해 반환한다
        mockMvc.perform(get("/api/_bench/posts")
                        .param("boardId", "1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray())
                .andExpect(jsonPath("$.posts[0].id").value(1))
                .andExpect(jsonPath("$.posts[0].title").value("벤치 제목"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(postFeedService).getPostsByBoardIdOffset(eq(1L), any(Pageable.class));
    }
}
