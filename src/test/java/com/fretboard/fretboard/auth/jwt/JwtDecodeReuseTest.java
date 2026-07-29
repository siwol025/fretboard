package com.fretboard.fretboard.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.global.datasource.UserContextFilter;
import com.fretboard.fretboard.global.datasource.UserContextHolder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Proves that a valid JWT is decoded EXACTLY ONCE across the whole servlet filter chain.
 *
 * <p>Both real filters are wired in their production order (UserContextFilter first,
 * JwtAuthFilter second) against a single shared mock {@link JwtTokenProvider}, then driven
 * through a {@link MockFilterChain}. Because both filters currently call
 * {@code decodeAccessToken} independently, the driver scenario fails today (2 decodes).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtDecodeReuseTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    private UserContextFilter userContextFilter;
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        userContextFilter = new UserContextFilter(tokenProvider);
        jwtAuthFilter = new JwtAuthFilter(new ObjectMapper(), tokenProvider);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    /**
     * Terminal servlet that records state observed at the deepest point of the chain
     * (i.e. after both filters have run their pre-chain logic), before the outer
     * UserContextFilter clears the holder in its finally block.
     */
    private static class CapturingServlet extends HttpServlet {
        private boolean reached;
        private final AtomicReference<Long> holderAtChain = new AtomicReference<>();
        private final AtomicReference<Object> memberIdAttrAtChain = new AtomicReference<>();

        @Override
        public void service(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            reached = true;
            holderAtChain.set(UserContextHolder.getUserId());
            memberIdAttrAtChain.set(((HttpServletRequest) req).getAttribute(JwtAuthFilter.MEMBER_ID_ATTRIBUTE));
        }
    }

    @Test
    void 유효토큰_보호경로_JWT_디코딩_1회만() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingServlet servlet = new CapturingServlet();
        MockFilterChain chain = new MockFilterChain(servlet, userContextFilter, jwtAuthFilter);
        given(tokenProvider.decodeAccessToken(anyString())).willReturn("7");

        chain.doFilter(request, response);

        assertThat(servlet.reached).isTrue();
        verify(tokenProvider, times(1)).decodeAccessToken(anyString());
    }

    @Test
    void 유효토큰_보호경로_memberId_속성_세팅_유지() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingServlet servlet = new CapturingServlet();
        MockFilterChain chain = new MockFilterChain(servlet, userContextFilter, jwtAuthFilter);
        given(tokenProvider.decodeAccessToken(anyString())).willReturn("7");

        chain.doFilter(request, response);

        assertThat(servlet.reached).isTrue();
        assertThat(servlet.memberIdAttrAtChain.get()).hasToString("7");
        assertThat(servlet.holderAtChain.get()).isEqualTo(7L);
    }

    @Test
    void 무효토큰_보호경로_여전히_401() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/posts");
        // no Authorization header -> missing token
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingServlet servlet = new CapturingServlet();
        MockFilterChain chain = new MockFilterChain(servlet, userContextFilter, jwtAuthFilter);

        chain.doFilter(request, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(servlet.reached).isFalse();
    }

    @Test
    void 화이트리스트경로_유효토큰_memberId_속성_미세팅_홀더는_세팅() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/posts/1");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingServlet servlet = new CapturingServlet();
        MockFilterChain chain = new MockFilterChain(servlet, userContextFilter, jwtAuthFilter);
        given(tokenProvider.decodeAccessToken(anyString())).willReturn("42");

        chain.doFilter(request, response);

        assertThat(servlet.reached).isTrue();
        // JwtAuthFilter is skipped on whitelisted paths -> attribute must remain unset
        assertThat(servlet.memberIdAttrAtChain.get()).isNull();
        // UserContextFilter still populates the holder
        assertThat(servlet.holderAtChain.get()).isEqualTo(42L);
    }
}
