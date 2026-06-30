package com.fretboard.fretboard.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.post.dto.request.PostNewRequest;
import com.fretboard.fretboard.post.dto.response.PostDetailResponse;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(content().string(not(emptyString())));
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
                        .with(req -> {
                            req.setAttribute("memberId", "1");
                            return req;
                        }))
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

    private String validPostRequestBody() throws Exception {
        return objectMapper.writeValueAsString(
                new PostNewRequest(1L, "테스트 제목", "테스트 내용")
        );
    }
}
