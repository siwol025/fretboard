package com.fretboard.fretboard.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.dto.request.LoginRequest;
import com.fretboard.fretboard.auth.dto.request.TokenReissueRequest;
import com.fretboard.fretboard.auth.dto.response.LoginResponse;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.auth.service.LoginService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = LoginController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LoginService loginService;

    @Test
    @DisplayName("로그인 성공 시 200 응답과 토큰 반환")
    void login_성공_200_토큰반환() throws Exception {
        // given
        LoginResponse response = LoginResponse.builder()
                .memberId(1L)
                .nickname("테스트유저")
                .accessToken("access-token-value")
                .refreshToken("refresh-token-value")
                .build();
        given(loginService.login(any())).willReturn(response);

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", "password123")
        );

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"));
    }

    @Test
    @DisplayName("비밀번호 틀림 시 401 응답")
    void login_비밀번호틀림_401_반환() throws Exception {
        // given
        given(loginService.login(any())).willThrow(new FretBoardException(ExceptionType.UNAUTHORIZED));

        String requestBody = objectMapper.writeValueAsString(
                new LoginRequest("testuser", "wrongpassword")
        );

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 200 응답")
    void reissueToken_성공_200_반환() throws Exception {
        // given
        LoginResponse response = LoginResponse.builder()
                .memberId(1L)
                .nickname("테스트유저")
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
        given(loginService.reissueToken(any())).willReturn(response);

        String requestBody = objectMapper.writeValueAsString(
                new TokenReissueRequest("valid-refresh-token")
        );

        // when & then
        mockMvc.perform(post("/api/login/reissue-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }
}
