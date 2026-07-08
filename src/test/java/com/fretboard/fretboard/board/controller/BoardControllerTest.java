package com.fretboard.fretboard.board.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.board.dto.request.BoardRequest;
import com.fretboard.fretboard.board.dto.response.BoardElementResponse;
import com.fretboard.fretboard.board.dto.response.BoardListResponse;
import com.fretboard.fretboard.board.domain.BoardType;
import com.fretboard.fretboard.board.service.BoardService;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BoardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class BoardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BoardService boardService;

    @Test
    @DisplayName("게시판 목록 조회 시 200 응답")
    void findBoards_목록조회_200() throws Exception {
        // given
        BoardListResponse response = new BoardListResponse(List.of(
                BoardElementResponse.builder()
                        .id(1L)
                        .title("자유게시판")
                        .description("자유롭게 이야기해요")
                        .slug("free")
                        .build()
        ));
        given(boardService.findBoards()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contents[0].id").value(1))
                .andExpect(jsonPath("$.contents[0].title").value("자유게시판"));
    }

    @Test
    @DisplayName("관리자가 게시판 생성 시 201 응답")
    void createBoard_관리자_201() throws Exception {
        // given
        given(boardService.createBoard(any(), any())).willReturn(1L);

        String requestBody = objectMapper.writeValueAsString(
                new BoardRequest("자유게시판", "자유롭게 이야기해요", "free", BoardType.WRITABLE)
        );

        // when & then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비인증 사용자가 게시판 생성 시 401 응답")
    void createBoard_비인증_401() throws Exception {
        // given
        String requestBody = objectMapper.writeValueAsString(
                new BoardRequest("자유게시판", "자유롭게 이야기해요", "free", BoardType.WRITABLE)
        );

        // when & then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 회원이 게시판 생성 시도하면 403 응답")
    void createBoard_일반회원_403() throws Exception {
        // given
        given(boardService.createBoard(any(), any()))
                .willThrow(new FretBoardException(ExceptionType.FORBIDDEN));

        String requestBody = objectMapper.writeValueAsString(
                new BoardRequest("자유게시판", "자유롭게 이야기해요", "free", BoardType.WRITABLE)
        );

        // when & then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 게시판 삭제 시 204 응답")
    void deleteBoard_관리자_204() throws Exception {
        // given
        willDoNothing().given(boardService).deleteBoard(any(), any());

        // when & then
        mockMvc.perform(delete("/api/boards/1")
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
