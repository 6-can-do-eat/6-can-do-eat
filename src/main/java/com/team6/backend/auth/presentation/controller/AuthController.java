package com.team6.backend.auth.presentation.controller;

import com.team6.backend.auth.presentation.dto.request.LoginRequest;
import com.team6.backend.auth.presentation.dto.request.SignupRequest;
import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.presentation.dto.response.UserResponse;
import com.team6.backend.auth.application.service.AuthService;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.TokenErrorCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<UserResponse>> signup(@RequestBody @Valid SignupRequest request) {
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(response));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<LoginResponse>> refresh(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = extractAccessToken(accessHeader);
        validateRefreshToken(refreshToken);
        return ResponseEntity.ok(SuccessResponse.ok(authService.refresh(accessToken, refreshToken)));
    }

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

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("토큰 테스트");
    }
}
