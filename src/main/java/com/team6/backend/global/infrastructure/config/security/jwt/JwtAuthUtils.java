package com.team6.backend.global.infrastructure.config.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthUtils {

    private final JwtUtil jwtUtil;

    /**
     * 블랙리스트에 없고 유효한 토큰이면 true 반환
     * 블랙리스트 체크 - 토큰 검증 - 타입 체크 순서로 실행
     */

    // isInvalidToken() → isValidToken()으로 긍정형으로 통일
    public boolean isValidToken(String token) {
        return !jwtUtil.isBlacklisted(token) && jwtUtil.validateToken(token) && "access".equals(jwtUtil.getType(token));
    }

    public Long getUserId(String token) {
        return jwtUtil.getUserId(token);
    }
}