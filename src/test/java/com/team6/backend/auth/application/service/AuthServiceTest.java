package com.team6.backend.auth.application.service;

import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.auth.presentation.dto.request.LoginRequest;
import com.team6.backend.auth.presentation.dto.request.SignupRequest;
import com.team6.backend.auth.presentation.dto.response.LoginResponse;
import com.team6.backend.auth.presentation.dto.response.UserResponse;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    // ========================
    // 테스트용 객체 생성 헬퍼
    // ========================

    private SignupRequest createSignupRequest(String username, String password, Role role, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "role", role);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private LoginRequest createLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    // ========================
    // signup 테스트
    // ========================

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = createSignupRequest("user1", "password123!", Role.CUSTOMER, "닉네임");

        given(userRepository.existsByUsername("user1")).willReturn(false);
        given(userRepository.existsByNickname("닉네임")).willReturn(false);
        given(passwordEncoder.encode("password123!")).willReturn("encodedPassword");

        User savedUser = new User("user1", "encodedPassword", Role.CUSTOMER, "닉네임");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponse response = authService.signup(request);

        // then
        assertThat(response.getUsername()).isEqualTo("user1");
        assertThat(response.getNickname()).isEqualTo("닉네임");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void signup_fail_duplicate_username() {
        // given
        SignupRequest request = createSignupRequest("user1", "password123!", Role.CUSTOMER, "닉네임");
        given(userRepository.existsByUsername("user1")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_fail_duplicate_nickname() {
        // given
        SignupRequest request = createSignupRequest("user1", "password123!", Role.CUSTOMER, "닉네임");
        given(userRepository.existsByUsername("user1")).willReturn(false);
        given(userRepository.existsByNickname("닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 허용되지 않은 Role")
    void signup_fail_invalid_role() {
        // given
        SignupRequest request = createSignupRequest("user1", "password123!", Role.MASTER, "닉네임");
        given(userRepository.existsByUsername("user1")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("허용되지 않은 권한입니다.");
    }

    // ========================
    // login 테스트
    // ========================

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = createLoginRequest("user1", "password123!");
        User user = new User("user1", "encodedPassword", Role.CUSTOMER, "닉네임");
        LoginResponse loginResponse = new LoginResponse("accessToken", "refreshToken");

        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123!", "encodedPassword")).willReturn(true);
        given(tokenService.issueTokens(user)).willReturn(loginResponse);

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    void login_fail_user_not_found() {
        // given
        LoginRequest request = createLoginRequest("user1", "password123!");
        given(userRepository.findByUsername("user1")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrong_password() {
        // given
        LoginRequest request = createLoginRequest("user1", "wrongPassword");
        User user = new User("user1", "encodedPassword", Role.CUSTOMER, "닉네임");

        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    // ========================
    // logout 테스트
    // ========================

    @Test
    @DisplayName("로그아웃 성공 - TokenService 메서드 호출 확인")
    void logout_success() {
        // given
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        // when
        authService.logout(accessToken, refreshToken);

        // then
        verify(tokenService).blacklistAccessToken(accessToken, "logout");
        verify(tokenService).deleteRefreshTokenByToken(refreshToken);
    }

    // ========================
    // refresh 테스트
    // ========================

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = new User("user1", "encodedPassword", Role.CUSTOMER, "닉네임");
        LoginResponse loginResponse = new LoginResponse("newAccessToken", "newRefreshToken");

        given(tokenService.validateAndGetUserId("refreshToken")).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(tokenService.issueTokens(user)).willReturn(loginResponse);

        // when
        LoginResponse response = authService.refresh("accessToken", "refreshToken");

        // then
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        verify(tokenService).blacklistAccessToken("accessToken", "refresh");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 존재하지 않는 유저")
    void refresh_fail_user_not_found() {
        // given
        UUID userId = UUID.randomUUID();
        given(tokenService.validateAndGetUserId("refreshToken")).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh("accessToken", "refreshToken"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 유저입니다.");
    }
}