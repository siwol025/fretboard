package com.fretboard.fretboard.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.comment.dto.request.CommentRequest;
import com.fretboard.fretboard.comment.dto.response.CommentResponse;
import com.fretboard.fretboard.comment.service.CommentService;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CommentService commentService;

    @Test
    @DisplayName("인증된 사용자가 댓글 추가 시 201 응답")
    void addComment_성공_201() throws Exception {
        // given
        given(commentService.addComment(any(), any(), any())).willReturn(1L);

        String requestBody = objectMapper.writeValueAsString(
                new CommentRequest("테스트 댓글입니다.")
        );

        // when & then
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비인증 사용자가 댓글 추가 시 401 응답")
    void addComment_비인증_401() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new CommentRequest("테스트 댓글입니다.")
        );

        // when & then
        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시글 댓글 목록 조회 시 200 응답")
    void findComments_성공_200() throws Exception {
        // given
        CommentResponse response = new CommentResponse(List.of());
        given(commentService.findComments(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents").isArray());
    }

    @Test
    @DisplayName("인증된 사용자가 댓글 수정 시 204 응답")
    void editComment_성공_204() throws Exception {
        // given
        willDoNothing().given(commentService).editComment(any(), any(), any());

        String requestBody = objectMapper.writeValueAsString(
                new CommentRequest("수정된 댓글입니다.")
        );

        // when & then
        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증된 사용자가 댓글 삭제 시 204 응답")
    void deleteComment_성공_204() throws Exception {
        // given
        willDoNothing().given(commentService).deleteComment(any(), any());

        // when & then
        mockMvc.perform(delete("/api/comments/1")
                        .with(authenticatedMember("1")))
                .andExpect(status().isNoContent());
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return req -> {
            req.setAttribute("memberId", memberId);
            return req;
        };
    }
}
