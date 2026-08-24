package com.fretboard.fretboard.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.global.auth.resolver.LoginMemberArgumentResolver;
import com.fretboard.fretboard.global.exception.ExceptionResponseHandler;
import com.fretboard.fretboard.member.dto.MemberEditRequest;
import com.fretboard.fretboard.member.dto.MemberEditResponse;
import com.fretboard.fretboard.member.dto.PasswordEditRequest;
import com.fretboard.fretboard.member.dto.SignupRequest;
import com.fretboard.fretboard.member.service.MemberService;
import com.fretboard.fretboard.post.dto.response.MyPostListResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthFilter.class, JwtTokenProvider.class}
        )
)
@Import({LoginMemberArgumentResolver.class, ExceptionResponseHandler.class})
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MemberService memberService;

    @MockBean
    PostService postService;

    @Test
    @DisplayName("회원가입 성공 시 204 응답")
    void signup_성공_204() throws Exception {
        // given
        willDoNothing().given(memberService).signup(any());

        String requestBody = objectMapper.writeValueAsString(
                new SignupRequest("testuser1", "password1A", "테스트닉네임")
        );

        // when & then
        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증된 사용자가 내 게시글 조회 시 200 응답")
    void getMyPosts_인증_200() throws Exception {
        // given
        MyPostListResponse response = new MyPostListResponse(
                List.of(), 0, 0, 0L, true, true
        );
        given(postService.findMyPosts(any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/members/mypage/posts")
                        .with(authenticatedMember("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isArray());
    }

    @Test
    @DisplayName("비인증 사용자가 내 게시글 조회 시 401 응답")
    void getMyPosts_비인증_401() throws Exception {
        // when & then
        mockMvc.perform(get("/api/members/mypage/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자가 비밀번호 변경 시 204 응답")
    void changePassword_성공_204() throws Exception {
        // given
        willDoNothing().given(memberService).updatePassword(any(), any());

        String requestBody = objectMapper.writeValueAsString(
                new PasswordEditRequest("currentPass1A", "newPassword1A")
        );

        // when & then
        mockMvc.perform(post("/api/members/password-changes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증된 사용자가 회원 정보 수정 시 200 응답")
    void editMemberInfo_성공_200() throws Exception {
        // given
        MemberEditResponse editResponse = MemberEditResponse.builder()
                .newNickname("새닉네임")
                .build();
        given(memberService.updateMemberInfo(any(), any())).willReturn(editResponse);

        String requestBody = objectMapper.writeValueAsString(
                new MemberEditRequest("새닉네임")
        );

        // when & then
        mockMvc.perform(post("/api/members/edit-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(authenticatedMember("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newNickname").value("새닉네임"));
    }

    private RequestPostProcessor authenticatedMember(String memberId) {
        return req -> {
            req.setAttribute("memberId", memberId);
            return req;
        };
    }
}
