package com.team6.backend.auth.application.service;

import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.domain.entity.User;
import com.team6.backend.global.infrastructure.redis.RedisService;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    // Refresh Token Redis 저장 공통 메서드 (login/refresh에서 재사용)
    public void saveRefreshToken(UUID userId, String refreshToken) {
        redisService.set(
                jwtUtil.getRefreshTokenKey(userId),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpiration())
        );
    }

    // JWT 생성 + Refresh Token Redis 저장 + 응답 반환 (login/refresh에서 재사용)
    public LoginResponse issueTokens(User user) {
        // JWT 생성
        String accessToken = jwtUtil.createAccessToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        // Refresh Token Redis 저장
        saveRefreshToken(user.getId(), refreshToken);

        // 응답 반환
        return new LoginResponse(accessToken, refreshToken);
    }

    // Refresh Token 검증 + Redis 비교 → userId 반환
    public UUID validateAndGetUserId(String refreshToken) {
        // Refresh Token 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        UUID userId = jwtUtil.getUserId(refreshToken);

        // Redis에 저장된 Refresh Token과 비교
        String stored = redisService.get(jwtUtil.getRefreshTokenKey(userId));
        if (!refreshToken.equals(stored)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        return userId;
    }

    // Access Token 블랙리스트 등록 (refresh/logout에서 재사용)
    public void blacklistAccessToken(String accessToken, String reason) {
        try {
            long expirationMillis =
                    jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();

            if (expirationMillis > 0) {
                redisService.set(
                        jwtUtil.getAccessTokenBlacklistKey(accessToken),
                        reason,
                        Duration.ofMillis(expirationMillis)
                );
            }
            // 만료됐으면 블랙리스트 등록 불필요 - 그냥 통과
        } catch (Exception e) {
            log.warn("[AUTH] {} 중 Access Token 블랙리스트 등록 실패 - {}", reason, e.getMessage());
        }
    }

    // Refresh Token Redis 삭제
    public void deleteRefreshToken(UUID userId) {
        redisService.delete(jwtUtil.getRefreshTokenKey(userId));
    }

    public void deleteRefreshTokenByToken(String refreshToken) {
        try {
            UUID userId = jwtUtil.getUserId(refreshToken);
            deleteRefreshToken(userId);
        } catch (Exception e) {
            log.warn("[AUTH] logout 중 Refresh Token 삭제 실패 - {}", e.getMessage());
        }
    }
}