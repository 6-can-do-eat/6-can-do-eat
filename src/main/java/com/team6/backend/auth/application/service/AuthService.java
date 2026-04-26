package com.team6.backend.auth.application.service;

import com.team6.backend.auth.presentation.dto.request.LoginRequest;
import com.team6.backend.auth.presentation.dto.request.SignupRequest;
import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.presentation.dto.response.UserResponse;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.AuthErrorCode;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // 닉네임 중복 체크 (공통 메서드 - 회원가입/닉네임 변경 시 재사용)
    private void validateNickname(String nickname) {
        if (nickname != null && userRepository.existsByNickname(nickname)) {
            throw new ApplicationException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
    }

    // Role 제한 (공통 메서드 - 권한 변경시 재사용)
    private void validateRole(Role role) {
        if (role != Role.CUSTOMER && role != Role.OWNER) {
            throw new ApplicationException(AuthErrorCode.INVALID_ROLE);
        }
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {

        // 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApplicationException(AuthErrorCode.DUPLICATE_USERNAME);
        }

        // Role 제한 (CUSTOMER 및 OWNER만 허용)
        validateRole(request.getRole());

        // 닉네임 중복 체크
        validateNickname(request.getNickname());

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // User 생성
        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .role(request.getRole())
                .nickname(request.getNickname())
                .build();

        // 저장
        User savedUser = userRepository.save(user);

        // DTO 반환
        return new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getNickname(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS));

        return tokenService.issueTokens(user);
    }

    @Transactional
    public LoginResponse refresh(String accessToken, String refreshToken) {

        UUID userId = tokenService.validateAndGetUserId(refreshToken);

        // 기존 Access Token 블랙리스트 등록
        tokenService.blacklistAccessToken(accessToken, "refresh");

        // 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(AuthErrorCode.USER_NOT_FOUND));

        // Refresh Token Rotation - 새 Refresh Token 발급 및 Redis 업데이트
        return tokenService.issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        tokenService.blacklistAccessToken(accessToken, "logout");
        tokenService.deleteRefreshTokenByToken(refreshToken);
    }
}