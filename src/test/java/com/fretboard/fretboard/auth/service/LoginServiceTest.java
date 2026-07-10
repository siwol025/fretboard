package com.fretboard.fretboard.auth.service;

import com.fretboard.fretboard.auth.dto.LoginInfoDto;
import com.fretboard.fretboard.auth.dto.request.LoginRequest;
import com.fretboard.fretboard.auth.dto.response.LoginResponse;
import com.fretboard.fretboard.auth.dto.response.TokenResponse;
import com.fretboard.fretboard.auth.encryptor.PasswordEncryptor;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import com.fretboard.fretboard.member.domain.Member;
import com.fretboard.fretboard.member.domain.Role;
import com.fretboard.fretboard.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final PasswordEncryptor passwordEncryptor = new PasswordEncryptor();

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(memberRepository, jwtTokenProvider, passwordEncryptor);
    }

    @Test
    void LoginService_login_올바른비밀번호_토큰반환() {
        // given
        String rawPassword = "rawPwd";
        String bcryptEncoded = passwordEncryptor.encode(rawPassword);

        Member member = Member.builder()
                .username("user")
                .password(bcryptEncoded)
                .nickname("nickname")
                .role(Role.USER)
                .build();

        given(memberRepository.findByUsername("user")).willReturn(Optional.of(member));
        given(jwtTokenProvider.createToken(any(LoginInfoDto.class)))
                .willReturn(new TokenResponse("access-token", "refresh-token"));

        // when
        LoginResponse response = loginService.login(new LoginRequest("user", rawPassword));

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void LoginService_login_잘못된비밀번호_PASSWORD_NOT_MATCH_예외() {
        // given
        String rawPassword = "rawPwd";
        String bcryptEncoded = passwordEncryptor.encode(rawPassword);

        Member member = Member.builder()
                .username("user")
                .password(bcryptEncoded)
                .nickname("nickname")
                .role(Role.USER)
                .build();

        given(memberRepository.findByUsername("user")).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> loginService.login(new LoginRequest("user", "wrongPassword")))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> assertThat(((FretBoardException) ex).getExceptionType())
                        .isEqualTo(ExceptionType.INVALID_PASSWORD));
    }
}
