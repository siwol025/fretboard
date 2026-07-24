package com.fretboard.fretboard.global.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserContextFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * FilterChain that captures the holder value AT THE MOMENT the chain runs.
     */
    private static class CapturingFilterChain implements FilterChain {
        private final AtomicBoolean invoked = new AtomicBoolean(false);
        private final AtomicReference<Long> userIdDuringChain = new AtomicReference<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invoked.set(true);
            userIdDuringChain.set(UserContextHolder.getUserId());
        }
    }

    @Test
    void 유효토큰_요청_홀더에_userId_세팅() throws ServletException, IOException {
        UserContextFilter filter = new UserContextFilter(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();
        given(tokenProvider.decodeAccessToken("good")).willReturn("1");

        filter.doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userIdDuringChain).hasValue(1L);
    }

    @Test
    void 토큰없는_요청_그대로_통과_홀더_비어있음() throws ServletException, IOException {
        UserContextFilter filter = new UserContextFilter(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userIdDuringChain).hasValue(null);
    }

    @Test
    void 무효토큰_요청_그대로_통과_홀더_비어있음() throws ServletException, IOException {
        UserContextFilter filter = new UserContextFilter(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();
        given(tokenProvider.decodeAccessToken("bad")).willThrow(new RuntimeException("invalid token"));

        filter.doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userIdDuringChain).hasValue(null);
    }

    @Test
    void 화이트리스트_읽기경로도_토큰있으면_세팅됨() throws ServletException, IOException {
        UserContextFilter filter = new UserContextFilter(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/posts/1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();
        given(tokenProvider.decodeAccessToken("good")).willReturn("42");

        filter.doFilter(request, response, chain);

        assertThat(chain.invoked).isTrue();
        assertThat(chain.userIdDuringChain).hasValue(42L);
    }

    @Test
    void 요청_처리_후_ThreadLocal_누수없음() throws ServletException, IOException {
        UserContextFilter filter = new UserContextFilter(tokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();
        given(tokenProvider.decodeAccessToken("good")).willReturn("1");

        filter.doFilter(request, response, chain);

        assertThat(UserContextHolder.getUserId()).isNull();
    }
}
