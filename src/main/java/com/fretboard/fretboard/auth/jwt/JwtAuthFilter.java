package com.fretboard.fretboard.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretboard.fretboard.auth.dto.HttpRequestInfo;
import com.fretboard.fretboard.global.exception.FretBoardErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * Request attribute holding the authenticated member id for downstream handlers.
     * Set by this filter only on non-whitelisted (protected) paths.
     */
    public static final String MEMBER_ID_ATTRIBUTE = "memberId";

    /**
     * Request attribute used to hand off an already-decoded member id from the earlier
     * {@link com.fretboard.fretboard.global.datasource.UserContextFilter} to this filter,
     * so a valid JWT is decoded exactly once across the whole filter chain.
     *
     * <p>Kept DISTINCT from {@link #MEMBER_ID_ATTRIBUTE}: this key is an internal
     * single-decode optimization cache, whereas {@code MEMBER_ID_ATTRIBUTE} is the
     * authentication contract exposed to downstream handlers and must only be populated
     * on protected paths (never on whitelisted read paths, where UserContextFilter still
     * populates this cache). Collapsing the two would leak the member id onto whitelisted
     * paths and break that distinction.
     */
    public static final String DECODED_MEMBER_ID_CACHE_ATTRIBUTE = "decodedMemberId";

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider tokenProvider;
    private final List<HttpRequestInfo> whiteList;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthFilter(
            ObjectMapper objectMapper,
            JwtTokenProvider tokenProvider
    ) {
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
        this.whiteList = List.of(
                new HttpRequestInfo(HttpMethod.POST, "/api/login"),
                new HttpRequestInfo(HttpMethod.POST, "/api/login/**"),
                new HttpRequestInfo(HttpMethod.POST, "/api/members/signup"),
                new HttpRequestInfo(HttpMethod.GET, "/api/posts"),
                new HttpRequestInfo(HttpMethod.GET, "/api/posts/**"),
                new HttpRequestInfo(HttpMethod.GET, "/api/boards"),
                new HttpRequestInfo(HttpMethod.GET, "/api/boards/**"),
                new HttpRequestInfo(HttpMethod.GET, "/actuator/prometheus"),
                new HttpRequestInfo(HttpMethod.GET, "/api/_bench/posts"),
                new HttpRequestInfo(HttpMethod.GET, "/api/_bench/**")
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isTokenBlank(token) || !token.startsWith("Bearer ")) {
            sendUnauthorizedResponse(request, response, "로그인을 해주세요.");
            return;
        }
        token = token.substring(7);
        try {
            // Reuse the id already decoded by UserContextFilter if present; only decode
            // here as a fallback. Do NOT simplify to an unconditional decode -- that would
            // decode the same valid JWT twice within one request.
            Object cached = request.getAttribute(DECODED_MEMBER_ID_CACHE_ATTRIBUTE);
            String memberId = (cached != null) ? (String) cached : tokenProvider.decodeAccessToken(token);
            request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
            filterChain.doFilter(request, response);
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            sendUnauthorizedResponse(request, response, e.getMessage());
        }
    }

    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        log.warn("UNAUTHORIZED_EXCEPTION :: message = {}", message);
        FretBoardErrorResponse errorResponse = new FretBoardErrorResponse(HttpStatus.UNAUTHORIZED, "로그인해주세요.");

        String origin = request.getHeader("Origin");
        String allowOrigin = (origin != null) ? origin : "*";

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
        response.getWriter()
                .write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        return isInWhiteList(method, requestURI);
    }

    private boolean isInWhiteList(String method, String url) {
        return whiteList.stream()
                .anyMatch(white -> white.method().matches(method) && antPathMatcher.match(white.urlPattern(), url));
    }

    private boolean isTokenBlank(String token) {
        return token == null || token.isBlank();
    }
}
