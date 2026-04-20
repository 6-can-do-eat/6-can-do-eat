package com.team6.backend.global.infrastructure.config.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * OncePerRequestFilter 상속 - 요청당 1회만 실행 보장
 * JwtFilter - JwtAuthUtils - JwtUtil (검증/블랙리스트)
 * 인증 실패 시: ExceptionTranslationFilter - JwtAuthenticationEntryPoint (401)
 *  요청 흐름:
 *  Authorization 헤더 확인 - 없으면 통과
 *  토큰 유효성 검사 - 실패 시 401
 *  userId 추출 후 SecurityContext 등록
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private List<GrantedAuthority> getAuthorities(String role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private final JwtAuthUtils jwtAuthUtils;

    // refresh는 Access Token 검증 스킵
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/v1/auth/refresh");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(AUTHORIZATION);

        // 토큰이 없는경우 인증 없이 통과
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // isValidToken() → false면 유효하지 않은 토큰 → 401 반환
        // JwtAuthUtils 참고
        if (!jwtAuthUtils.isValidToken(token)) {
            log.warn("[JWT] 유효하지 않은 토큰 - uri={}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
            return;
        }

        // 사용자 정보 꺼내기
        UUID userId = jwtAuthUtils.getUserId(token);
        String role = jwtAuthUtils.getRole(token);
        List<GrantedAuthority> authorities = getAuthorities(role);

        // 인증 객체 생성 (권한 없이)
        UsernamePasswordAuthenticationToken auth =

                /*
                 * JWT 인증 객체 생성
                 * token에서 userId 추출 후 principal로 사용
                 * credentials는 JWT 기반이라 별도 비밀번호 없음 (null 처리)
                 * authorities는 token에서 추출한 role로 설정 (ROLE_ 접두사 포함)
                 */
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // 새 SecurityContext 생성 후 인증 정보 등록 (이전 정보 덮어쓰기 방지)
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}