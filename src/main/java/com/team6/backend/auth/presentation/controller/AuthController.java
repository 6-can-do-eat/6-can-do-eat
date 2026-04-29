package com.team6.backend.auth.presentation.controller;

import com.team6.backend.auth.presentation.dto.request.LoginRequest;
import com.team6.backend.auth.presentation.dto.request.SignupRequest;
import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.presentation.dto.response.UserResponse;
import com.team6.backend.auth.application.service.AuthService;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.TokenErrorCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private String extractAccessToken(String accessHeader) {
        if (accessHeader == null || !accessHeader.startsWith("Bearer ")) {
            throw new ApplicationException(TokenErrorCode.INVALID_ACCESS_TOKEN);
        }
        return accessHeader.substring(7);
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApplicationException(TokenErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<UserResponse>> signup(@RequestBody @Valid SignupRequest request) {
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<LoginResponse>> refresh(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = extractAccessToken(accessHeader);
        validateRefreshToken(refreshToken);
        return ResponseEntity.ok(SuccessResponse.ok(authService.refresh(accessToken, refreshToken)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        // Security에서 검증하지만 방어적 처리
        String accessToken = extractAccessToken(accessHeader);
        validateRefreshToken(refreshToken);
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }
}
