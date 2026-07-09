package com.fretboard.fretboard.auth.jwt;

import com.fretboard.fretboard.auth.dto.LoginInfoDto;
import com.fretboard.fretboard.auth.dto.response.TokenResponse;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String ACCESS_SECRET = "test-secret-key-32chars-min-length!!";
    private static final String REFRESH_SECRET = "refresh-secret-key-32chars-min-length!!";

    private JwtTokenProvider provider(long accessExpiration) {
        return new JwtTokenProvider(ACCESS_SECRET, REFRESH_SECRET, accessExpiration, 3600000L);
    }

    @Test
    void createToken_후_decodeAccessToken으로_memberId_추출_성공() {
        JwtTokenProvider jwtTokenProvider = provider(3600000L);
        LoginInfoDto loginInfo = new LoginInfoDto(42L, "testuser", "testnick");

        TokenResponse token = jwtTokenProvider.createToken(loginInfo);
        String memberId = jwtTokenProvider.decodeAccessToken(token.accessToken());

        assertThat(memberId).isEqualTo("42");
    }

    @Test
    void 만료된_accessToken_decode시_EXPIRED_TOKEN_예외() {
        JwtTokenProvider expiredProvider = provider(-1L);
        LoginInfoDto loginInfo = new LoginInfoDto(1L, "user", "nick");

        TokenResponse token = expiredProvider.createToken(loginInfo);

        assertThatThrownBy(() -> expiredProvider.decodeAccessToken(token.accessToken()))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> assertThat(((FretBoardException) ex).getExceptionType())
                        .isEqualTo(ExceptionType.EXPIRED_TOKEN));
    }

    @Test
    void 잘못된_형식의_토큰_decode시_INVALID_TOKEN_예외() {
        JwtTokenProvider jwtTokenProvider = provider(3600000L);

        assertThatThrownBy(() -> jwtTokenProvider.decodeAccessToken("invalid.token.string"))
                .isInstanceOf(FretBoardException.class)
                .satisfies(ex -> assertThat(((FretBoardException) ex).getExceptionType())
                        .isEqualTo(ExceptionType.INVALID_TOKEN));
    }
}
