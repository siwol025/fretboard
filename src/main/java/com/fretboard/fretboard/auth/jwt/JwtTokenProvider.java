package com.fretboard.fretboard.auth.jwt;

import com.fretboard.fretboard.auth.dto.response.TokenResponse;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String MEMBER_ID_KEY = "id";

    private final String accessSecretKey;
    private final String refreshSecretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtTokenProvider(
            @Value("${security.jwt.token.secret-key}") String accessSecretKey,
            @Value("${security.jwt.refresh.secret-key}") String refreshSecretKey,
            @Value("${security.jwt.token.expire-length}") long accessExpiration,
            @Value("${security.jwt.refresh.expire-length}") long refreshExpiration
    ) {
        this.accessSecretKey = accessSecretKey;
        this.accessExpiration = accessExpiration;
        this.refreshSecretKey = refreshSecretKey;
        this.refreshExpiration = refreshExpiration;
    }

    public TokenResponse createToken(Long memberId) {
        String accessToken = createToken(memberId, accessSecretKey, accessExpiration);
        String refreshToken = createToken(memberId, refreshSecretKey, refreshExpiration);
        return new TokenResponse(accessToken, refreshToken);
    }

    private String createToken(Long memberId, String secretKey, long expiration) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(memberId.toString())
                .claim(MEMBER_ID_KEY, memberId)
                .setExpiration(validity)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()), SignatureAlgorithm.HS256)
                .setHeaderParam("typ", "JWT")
                .compact();
    }

    public String decodeAccessToken(String token) {
        return decode(token, accessSecretKey);
    }

    public String decodeRefreshToken(String token) {
        return decode(token, refreshSecretKey);
    }

    private String decode(String token, String secretKey) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get(MEMBER_ID_KEY)
                    .toString();
        } catch (ExpiredJwtException exception) {
            throw new FretBoardException(ExceptionType.EXPIRED_TOKEN);
        } catch (Exception exception) {
            throw new FretBoardException(ExceptionType.INVALID_TOKEN);
        }
    }
}
