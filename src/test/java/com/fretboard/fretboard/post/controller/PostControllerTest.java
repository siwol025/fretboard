package com.fretboard.fretboard.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
import com.fretboard.fretboard.post.dto.response.PostListResponse;
import com.fretboard.fretboard.post.service.PostFeedService;
import com.fretboard.fretboard.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostController 슬라이스 테스트.
 * JwtAuthFilter/JwtTokenProvider는 서블릿 필터 계층이므로 @WebMvcTest 컨텍스트에서 제외한다.
 * LoginMemberArgumentResolver와 ExceptionResponseHandler는 컨트롤러 동작에 필요하므로 명시적으로 임포트한다.
 */
@WebMvcTest(
        controllers = PostController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostService postService;

    @MockBean
    PostFeedService postFeedService;

    @Test
    @DisplayName("존재하는 게시글 조회 시 200 응답")
    void getPost_존재하는_게시글_200_응답() throws Exception {
        // given
        PostDetailResponse response = PostDetailResponse.builder()
                .id(1L)
                .title("테스트 제목")
                .content("테스트 내용")
                .authorId(1L)
                .author("작성자")
                .createdAt(LocalDateTime.now())
                .viewCount(0L)
                .boardId(1L)
                .boardTitle("자유게시판")
                .build();
        given(postService.getPostDetail(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("테스트 제목"))
                .andExpect(jsonPath("$.author").value("작성자"))
                .andExpect(jsonPath("$.boardTitle").value("자유게시판"))
                .andExpect(jsonPath("$.viewCount").value(0));
    }

    @Test
    @DisplayName("인증된 사용자가 게시글 작성 시 201 응답")
    void createPost_인증된_사용자_201_응답() throws Exception {
        // given
        given(postService.addPost(any(), any())).willReturn(1L);
        String requestBody = validPostRequestBody();

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비인증 사용자가 게시글 작성 시 401 응답")
    void createPost_비인증_사용자_401_응답() throws Exception {
        // given
        String requestBody = validPostRequestBody();

        // when & then — memberId attribute 없이 요청하면 LoginMemberArgumentResolver가 401 던짐
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 404 응답")
    void getPost_존재하지_않는_게시글_404_응답() throws Exception {
        // given
        given(postService.getPostDetail(999L)).willThrow(new FretBoardException(ExceptionType.POST_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("게시글 작성 시 제목이 빈값이면 400 응답")
    void addPost_제목_빈값이면_400_응답() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new PostNewRequest(1L, "", "내용")
        );

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시판 목록 조회 시 PostFeedService를 통해 반환한다")
    void getPostsByBoardId_게시판목록_PostFeedService_통해_반환() throws Exception {
        // given
        given(postFeedService.getPostsByBoardId(eq(1L), any(Pageable.class)))
                .willReturn(PostListResponse.of(Page.empty()));

        // when & then
        mockMvc.perform(get("/api/posts").param("boardId", "1"))
                .andExpect(status().isOk());
        verify(postFeedService).getPostsByBoardId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("인기글 조회 시 PostFeedService를 통해 반환한다")
    void getBestPosts_인기글_PostFeedService_통해_반환() throws Exception {
        // given
        given(postFeedService.getMostPosts()).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/posts/best"))
                .andExpect(status().isOk());
        verify(postFeedService).getMostPosts();
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return req -> {
            req.setAttribute("memberId", memberId);
            return req;
        };
    }

    private String validPostRequestBody() throws Exception {
        return objectMapper.writeValueAsString(
                new PostNewRequest(1L, "테스트 제목", "테스트 내용")
        );
    }
}
