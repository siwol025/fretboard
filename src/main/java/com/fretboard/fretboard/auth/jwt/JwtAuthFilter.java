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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String MEMBER_ID_ATTRIBUTE = "memberId";

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider tokenProvider;
    private final List<HttpRequestInfo> whiteList;

    public JwtAuthFilter(
            ObjectMapper objectMapper,
            JwtTokenProvider tokenProvider
    ) {
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
        this.whiteList = List.of(
                new HttpRequestInfo(HttpMethod.POST, "/login"),
                new HttpRequestInfo(HttpMethod.POST, "/members/**")
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isTokenBlank(token)) {
            sendUnauthorizedResponse(response, "로그인을 해주세요.");
            return;
        }
        token = token.split("Bearer|bearer")[1];
        try {
            String memberId = tokenProvider.decodeAccessToken(token);
            request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            sendUnauthorizedResponse(response, e.getMessage());
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        log.warn("UNAUTHORIZED_EXCEPTION :: message = {}", message);
        FretBoardErrorResponse errorResponse = new FretBoardErrorResponse(HttpStatus.UNAUTHORIZED, "로그인해주세요.");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
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
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        return whiteList.stream()
                .anyMatch(white -> white.method().matches(method) && antPathMatcher.match(white.urlPattern(), url));
    }

    private boolean isTokenBlank(String token) {
        return token == null || token.isBlank();
    }
}
