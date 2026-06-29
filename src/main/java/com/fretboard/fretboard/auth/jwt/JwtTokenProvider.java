package com.fretboard.fretboard.auth.jwt;

import com.fretboard.fretboard.auth.dto.LoginInfoDto;
import com.fretboard.fretboard.auth.dto.response.TokenResponse;
import com.fretboard.fretboard.global.exception.ExceptionType;
import com.fretboard.fretboard.global.exception.FretBoardException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String MEMBER_ID_KEY = "id";
    private static final String MEMBER_USERNAME_KEY = "username";
    private static final String MEMBER_NICKNAME_KEY = "nickname";

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

    public TokenResponse createToken(LoginInfoDto loginInfo) {
        String accessToken = createToken(loginInfo, accessSecretKey, accessExpiration);
        String refreshToken = createToken(loginInfo, refreshSecretKey, refreshExpiration);
        return new TokenResponse(accessToken, refreshToken);
    }

    private String createToken(LoginInfoDto loginInfo, String secretKey, long expiration) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(loginInfo.memberId().toString())
                .claim(MEMBER_ID_KEY, loginInfo.memberId())
                .claim(MEMBER_USERNAME_KEY, loginInfo.username())
                .claim(MEMBER_NICKNAME_KEY, loginInfo.nickname())
                .expiration(validity)
                .signWith(key)
                .header().add("typ", "JWT").and()
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
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(MEMBER_ID_KEY)
                    .toString();
        } catch (ExpiredJwtException exception) {
            throw new FretBoardException(ExceptionType.EXPIRED_TOKEN);
        } catch (Exception exception) {
            throw new FretBoardException(ExceptionType.INVALID_TOKEN);
        }
    }
}
