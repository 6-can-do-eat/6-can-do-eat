package com.team6.backend.global.infrastructure.config.security.config;

import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 권한 설정
                // GCP 헬스체크 추가
                .authorizeHttpRequests(auth -> auth
                        // TODO: 운영 환경 배포 시 .hasRole("MASTER") 또는 접근 제한 설정 필요
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/signup", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/toss/success", "/api/v1/toss/fail").permitAll()

                        // [보안 강화] 이중 안전을 위해 추후 (/**)를 제거하고 명확한 엔드포인트를 각각 추가해야 함
                        // /api/v1/** 로 뭉뚱그릴 경우, 정의되지 않은 경로로의 비정상적인 요청까지 인증 필터를 탈 수 있음
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll()
                )

                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}