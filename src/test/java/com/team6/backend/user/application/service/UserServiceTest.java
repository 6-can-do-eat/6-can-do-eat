package com.team6.backend.user.application.service;

import com.team6.backend.auth.application.service.TokenService;
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
        given(userRepository.findByUsernameAndDeletedAtIsNull("testuser"))
                .willReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = userService.getUserDetail("testuser");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull("testuser");
    }

    @Test
    @DisplayName("사용자 상세 조회 실패 - 사용자를 찾을 수 없음")
    void getUserDetail_notFound() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(anyString()))
                .willReturn(Optional.empty());

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.getUserDetail("nonexistentUser"));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(anyString());
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
        String originalUsername = "testuser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(originalUsername))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(originalUsername, userInfoRequest, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(originalUsername);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - MASTER가 다른 사용자 수정")
    void updateUser_masterUpdateOtherUser_success() {
        // Given
        String targetUsername = "testuser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(targetUsername))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(targetUsername, userInfoRequest, masterUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(targetUsername);
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 권한 없음 (일반 사용자가 다른 사용자 수정 시도)")
    void updateUser_forbidden() {
        // Given
        String anotherUsername = "another";

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUser(anotherUsername, userInfoRequest, testUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        then(userRepository).should(never()).findByUsernameAndDeletedAtIsNull(anyString());
    }

    @Test
    @DisplayName("사용자 권한 변경 성공")
    void updateUserRole_success() {
        // Given
        User targetUser = new User(UUID.randomUUID(), "targetUser", "pw", Role.CUSTOMER, "Target Nickname");
        String targetUsername = "targetUser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(targetUsername))
                .willReturn(Optional.of(targetUser));
        given(userRepository.saveAndFlush(any(User.class))).willReturn(targetUser);

        // When
        UserInfoResponse response = userService.updateUserRole(targetUsername, Role.MASTER, masterUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.MASTER);
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(targetUsername);
        then(userRepository).should(times(1)).saveAndFlush(targetUser);
        then(tokenService).should(times(1)).deleteRefreshToken(targetUser.getId());
        then(redisService).should(times(1)).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - 변경하려는 권한이 현재 권한과 동일")
    void updateUserRole_invalidInput_sameRole() {
        // Given
        String username = "testuser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(username))
                .willReturn(Optional.of(testUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(username, Role.CUSTOMER, masterUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - MASTER가 본인 권한 변경 시도")
    void updateUserRole_forbidden_masterSelfChange() {
        // Given
        String username = "master";
        given(userRepository.findByUsernameAndDeletedAtIsNull(username))
                .willReturn(Optional.of(masterUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(username, Role.CUSTOMER, masterUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
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

    @Test
    @DisplayName("사용자 권한 검증 실패 - 사용자를 찾을 수 없음")
    void validateUserRole_notFound() {
        // Given
        given(userRepository.findById(any(UUID.class))).willReturn(Optional.empty());

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.validateUserRole(UUID.randomUUID(), Role.CUSTOMER));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 권한 검증 실패 - 권한 불일치")
    void validateUserRole_forbidden() {
        // Given
        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.validateUserRole(testUser.getId(), Role.MASTER));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("사용자 삭제 성공 - 일반 사용자 본인 삭제")
    void deleteUser_selfDelete_success() {
        // Given
        String username = "testuser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(username))
                .willReturn(Optional.of(testUser));

        // When
        userService.deleteUser(username, testUser);

        // Then
        assertThat(testUser.getDeletedAt()).isNotNull();
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(username);
        then(tokenService).should(times(1)).deleteRefreshToken(testUser.getId());
        then(redisService).should(times(1)).delete(anyString());
    }

    @Test
    @DisplayName("사용자 삭제 성공 - MASTER가 다른 사용자 삭제")
    void deleteUser_masterDeleteOtherUser_success() {
        // Given
        String username = "testuser";
        given(userRepository.findByUsernameAndDeletedAtIsNull(username))
                .willReturn(Optional.of(testUser));

        // When
        userService.deleteUser(username, masterUser);

        // Then
        assertThat(testUser.getDeletedAt()).isNotNull();
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(username);
        then(tokenService).should(times(1)).deleteRefreshToken(testUser.getId());
        then(redisService).should(times(1)).delete(anyString());
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 권한 없음 (일반 사용자가 다른 사용자 삭제 시도)")
    void deleteUser_forbidden() {
        // Given
        String anotherUsername = "another";

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.deleteUser(anotherUsername, testUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        then(userRepository).should(never()).findByUsernameAndDeletedAtIsNull(anyString());
    }
}
