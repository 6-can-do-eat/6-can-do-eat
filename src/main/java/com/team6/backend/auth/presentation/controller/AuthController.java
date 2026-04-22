package com.team6.backend.auth.presentation.controller;

import com.team6.backend.auth.presentation.dto.request.LoginRequest;
import com.team6.backend.auth.presentation.dto.request.SignupRequest;
import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.presentation.dto.response.UserResponse;
import com.team6.backend.auth.application.service.AuthService;
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
        String accessToken = (accessHeader != null && accessHeader.startsWith("Bearer "))
                ? accessHeader.substring(7)
                : null;
        return ResponseEntity.ok(SuccessResponse.ok(authService.refresh(accessToken, refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String accessHeader,
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        String accessToken = accessHeader.substring(7);
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("토큰 테스트");
    }
}
