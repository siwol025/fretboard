package com.fretboard.fretboard.global.datasource;

import com.fretboard.fretboard.auth.jwt.JwtAuthFilter;
import com.fretboard.fretboard.auth.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates {@link UserContextHolder} with the caller's member id on a best-effort basis.
 *
 * <p>Registered as a {@link Component} so Spring Boot auto-wires it into the servlet filter chain
 * for every request. It runs BEFORE {@link com.fretboard.fretboard.auth.jwt.JwtAuthFilter}
 * (see {@link Order}) so the holder is populated even for whitelisted read paths that
 * {@code JwtAuthFilter} skips, and regardless of {@code JwtAuthFilter}'s reject-on-missing-token
 * behavior.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserContextFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public UserContextFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            populateUserContext(request);
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private void populateUserContext(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return;
        }
        try {
            String memberId = tokenProvider.decodeAccessToken(authorization.substring(BEARER_PREFIX.length()));
            // Stash the decoded id so the later JwtAuthFilter can reuse it instead of
            // decoding the same JWT a second time (single-decode across the filter chain).
            request.setAttribute(JwtAuthFilter.DECODED_MEMBER_ID_CACHE_ATTRIBUTE, memberId);
            UserContextHolder.set(Long.valueOf(memberId));
        } catch (Exception ignored) {
            // best-effort: never reject the request when the token is missing/invalid
        }
    }
}
