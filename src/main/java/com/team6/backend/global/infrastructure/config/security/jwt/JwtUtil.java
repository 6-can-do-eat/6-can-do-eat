package com.team6.backend.global.infrastructure.config.security.jwt;

import com.team6.backend.global.infrastructure.redis.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RedisService redisService;

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    // access token 만료 시간 반환
    @Getter
    @Value("${JWT_ACCESS_TOKEN_EXPIRATION}")
    private long accessTokenExpiration;

    @Getter
    @Value("${JWT_REFRESH_TOKEN_EXPIRATION}")
    private long refreshTokenExpiration;

    /**
     * JWT 서명/검증에 사용되는 실제 Key 객체
     * (문자열 secretKey → 암호화 키 객체로 변환)
     */
    private SecretKey key;
    private static final String BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_PREFIX = "refresh:";

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Redis에서 블랙리스트를 식별하기 위한 키 생성
     * blacklist:access:{token}
     */
    public String getAccessTokenBlacklistKey(String token) {
        return BLACKLIST_PREFIX + token;
    }

    public String getRefreshTokenKey(UUID userId) {
        return REFRESH_PREFIX + userId;
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }


    public String createAccessToken(UUID userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 토큰 타입 반환
    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    // 토큰 검증 - 서명 위조 / 만료 / 파싱 실패하면 false
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // Redis 블랙리스트에 존재하면 true
    public boolean isBlacklisted(String token) {
        return redisService.exists(getAccessTokenBlacklistKey(token));
    }
}