package com.team6.backend.user.application.service;

import com.team6.backend.auth.application.service.TokenService;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.redis.RedisService;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.UserInfoRepository;
import com.team6.backend.user.presentation.dto.request.UserInfoRequest;
import com.team6.backend.user.presentation.dto.response.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserInfoRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RedisService redisService;

    @Mock
    private SecurityUtils securityUtils;

    private User testUser;
    private User masterUser;
    private UserInfoRequest userInfoRequest;

    @BeforeEach
    void setUp() {
        testUser = new User(UUID.randomUUID(), "testuser", "encodedPassword", Role.CUSTOMER, "Test Nickname");
        masterUser = new User(UUID.randomUUID(), "master", "masterPassword", Role.MASTER, "Master Nickname");
        userInfoRequest = new UserInfoRequest("newUsername", "newPassword");
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUserDetail_success() {
        // Given
        given(securityUtils.getCurrentUserId()).willReturn(testUser.getId());
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);
        given(userRepository.findByIdAndDeletedAtIsNull(testUser.getId()))
                .willReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = userService.getUserDetail(testUser.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        then(userRepository).should(times(1)).findByIdAndDeletedAtIsNull(testUser.getId());
    }

    @Test
    @DisplayName("사용자 상세 조회 실패 - 사용자를 찾을 수 없음")
    void getUserDetail_notFound() {
        // Given
        UUID userId = UUID.randomUUID();
        given(securityUtils.getCurrentUserId()).willReturn(userId);
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);
        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.empty());

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.getUserDetail(userId));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 목록 조회 성공 - 키워드 없음")
    void getUsers_noKeyword_success() {
        // Given
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));
        given(userRepository.findAllByDeletedAtIsNull(any(Pageable.class))).willReturn(userPage);

        // When
        Page<UserInfoResponse> responsePage = userService.getUsers(null, 0, 10, "username", true);

        // Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        then(userRepository).should(times(1)).findAllByDeletedAtIsNull(any(Pageable.class));
    }

    @Test
    @DisplayName("사용자 목록 조회 성공 - 키워드 있음")
    void getUsers_withKeyword_success() {
        // Given
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser));
        given(userRepository.findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                anyString(), anyString(), any(Pageable.class))).willReturn(userPage);

        // When
        Page<UserInfoResponse> responsePage = userService.getUsers("test", 0, 10, "username", true);

        // Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        then(userRepository).should(times(1)).findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 일반 사용자 본인 수정")
    void updateUser_selfUpdate_success() {
        // Given
        given(securityUtils.getCurrentUserId()).willReturn(testUser.getId());
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);
        given(userRepository.findByIdAndDeletedAtIsNull(testUser.getId()))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(testUser.getId(), userInfoRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - MASTER가 다른 사용자 수정")
    void updateUser_masterUpdateOtherUser_success() {
        // Given
        given(securityUtils.getCurrentUserId()).willReturn(masterUser.getId());
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);
        given(userRepository.findByIdAndDeletedAtIsNull(testUser.getId()))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(testUser.getId(), userInfoRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
    }

    @Test
    @DisplayName("사용자 권한 변경 성공")
    void updateUserRole_success() {
        // Given
        User targetUser = new User(UUID.randomUUID(), "targetUser", "pw", Role.CUSTOMER, "Target Nickname");
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);
        given(securityUtils.getCurrentUserId()).willReturn(masterUser.getId());
        given(userRepository.findByIdAndDeletedAtIsNull(targetUser.getId()))
                .willReturn(Optional.of(targetUser));

        // When
        UserInfoResponse response = userService.updateUserRole(targetUser.getId(), Role.OWNER);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.OWNER);
        then(userRepository).should(times(1)).saveAndFlush(targetUser);
        then(tokenService).should(times(1)).deleteRefreshToken(targetUser.getId());
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - 일반 사용자가 시도")
    void updateUserRole_forbidden_notMaster() {
        // Given
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // When & Then
        assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(testUser.getId(), Role.OWNER));
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - 본인 권한 변경 시도")
    void updateUserRole_forbidden_selfChange() {
        // Given
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);
        given(securityUtils.getCurrentUserId()).willReturn(masterUser.getId());
        given(userRepository.findByIdAndDeletedAtIsNull(masterUser.getId()))
                .willReturn(Optional.of(masterUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(masterUser.getId(), Role.CUSTOMER));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_success() {
        // Given
        given(securityUtils.getCurrentUserId()).willReturn(testUser.getId());
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);
        given(userRepository.findByIdAndDeletedAtIsNull(testUser.getId()))
                .willReturn(Optional.of(testUser));

        // When
        userService.deleteUser(testUser.getId());

        // Then
        assertThat(testUser.getDeletedAt()).isNotNull();
        then(tokenService).should(times(1)).deleteRefreshToken(testUser.getId());
    }

    @Test
    @DisplayName("사용자 권한 검증 성공")
    void validateUserRole_success() {
        // Given
        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));

        // When
        userService.validateUserRole(testUser.getId(), Role.CUSTOMER);

        // Then
        then(userRepository).should(times(1)).findById(testUser.getId());
    }
}
