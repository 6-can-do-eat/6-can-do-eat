package com.team6.backend.user.application.service;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    private userInfoRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User masterUser;
    private UserInfoRequest userInfoRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("encodedPassword")
                .nickname("Test Nickname")
                .role(Role.CUSTOMER)
                .build();

        masterUser = User.builder()
                .id(UUID.randomUUID())
                .username("master")
                .password("masterPassword")
                .nickname("Master Nickname")
                .role(Role.MASTER)
                .build();

        userInfoRequest = new UserInfoRequest("newUsername", "newPassword");
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getUserDetail_success() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));

        // When
        UserInfoResponse response = userService.getUserDetail(testUser.getUsername());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getNickname()).isEqualTo(testUser.getNickname());
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(testUser.getUsername());
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
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser), pageable, 1);
        given(userRepository.findAllByDeletedAtIsNull(pageable)).willReturn(userPage);

        // When
        Page<UserInfoResponse> responsePage = userService.getUsers(null, 0, 10, "username", true);

        // Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        assertThat(responsePage.getContent().get(0).getUsername()).isEqualTo(testUser.getUsername());
        then(userRepository).should(times(1)).findAllByDeletedAtIsNull(pageable);
        then(userRepository).should(never()).findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("사용자 목록 조회 성공 - 키워드 있음")
    void getUsers_withKeyword_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(testUser), pageable, 1);
        given(userRepository.findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                anyString(), anyString(), any(Pageable.class))).willReturn(userPage);

        // When
        Page<UserInfoResponse> responsePage = userService.getUsers("test", 0, 10, "username", true);

        // Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        assertThat(responsePage.getContent().get(0).getUsername()).isEqualTo(testUser.getUsername());
        then(userRepository).should(times(1)).findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                anyString(), anyString(), any(Pageable.class));
        then(userRepository).should(never()).findAllByDeletedAtIsNull(any(Pageable.class));
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 일반 사용자 본인 수정")
    void updateUser_selfUpdate_success() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(testUser.getUsername(), userInfoRequest, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword"); // Verify password updated
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(testUser.getUsername());
        then(passwordEncoder).should(times(1)).encode(userInfoRequest.getPassword());
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - MASTER가 다른 사용자 수정")
    void updateUser_masterUpdateOtherUser_success() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));
        given(passwordEncoder.encode(userInfoRequest.getPassword())).willReturn("newEncodedPassword");

        // When
        UserInfoResponse response = userService.updateUser(testUser.getUsername(), userInfoRequest, masterUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo(userInfoRequest.getUsername());
        assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(testUser.getUsername());
        then(passwordEncoder).should(times(1)).encode(userInfoRequest.getPassword());
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 권한 없음 (일반 사용자가 다른 사용자 수정 시도)")
    void updateUser_forbidden() {
        // Given
        User anotherUser = User.builder().username("another").build();
        given(userRepository.findByUsernameAndDeletedAtIsNull(anotherUser.getUsername()))
                .willReturn(Optional.of(anotherUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUser(anotherUser.getUsername(), userInfoRequest, testUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        then(userRepository).should(never()).save(any(User.class)); // Ensure no save operation
    }

    @Test
    @DisplayName("사용자 권한 변경 성공")
    void updateUserRole_success() {
        // Given
        User targetUser = User.builder()
                .id(UUID.randomUUID())
                .username("targetUser")
                .password("pw")
                .nickname("Target Nickname")
                .role(Role.CUSTOMER)
                .build();
        given(userRepository.findByUsernameAndDeletedAtIsNull(targetUser.getUsername()))
                .willReturn(Optional.of(targetUser));
        given(userRepository.saveAndFlush(any(User.class))).willReturn(targetUser); // Mock saveAndFlush

        // When
        UserInfoResponse response = userService.updateUserRole(targetUser.getUsername(), Role.MASTER, masterUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.MASTER);
        assertThat(targetUser.getRole()).isEqualTo(Role.MASTER); // Verify role updated
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(targetUser.getUsername());
        then(userRepository).should(times(1)).saveAndFlush(targetUser);
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - 변경하려는 권한이 현재 권한과 동일")
    void updateUserRole_invalidInput_sameRole() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(testUser.getUsername(), Role.CUSTOMER, masterUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
        then(userRepository).should(never()).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("사용자 권한 변경 실패 - MASTER가 본인 권한 변경 시도")
    void updateUserRole_forbidden_masterSelfChange() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(masterUser.getUsername()))
                .willReturn(Optional.of(masterUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.updateUserRole(masterUser.getUsername(), Role.CUSTOMER, masterUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        then(userRepository).should(never()).saveAndFlush(any(User.class));
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
        then(userRepository).should(times(1)).findById(any(UUID.class));
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
        then(userRepository).should(times(1)).findById(testUser.getId());
    }

    @Test
    @DisplayName("사용자 삭제 성공 - 일반 사용자 본인 삭제")
    void deleteUser_selfDelete_success() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));

        // When
        userService.deleteUser(testUser.getUsername(), testUser);

        // Then
        assertThat(testUser.getDeletedAt()).isNotNull();
        assertThat(testUser.getDeletedBy()).isEqualTo(testUser.getUsername());
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(testUser.getUsername());
    }

    @Test
    @DisplayName("사용자 삭제 성공 - MASTER가 다른 사용자 삭제")
    void deleteUser_masterDeleteOtherUser_success() {
        // Given
        given(userRepository.findByUsernameAndDeletedAtIsNull(testUser.getUsername()))
                .willReturn(Optional.of(testUser));

        // When
        userService.deleteUser(testUser.getUsername(), masterUser);

        // Then
        assertThat(testUser.getDeletedAt()).isNotNull();
        assertThat(testUser.getDeletedBy()).isEqualTo(masterUser.getUsername());
        then(userRepository).should(times(1)).findByUsernameAndDeletedAtIsNull(testUser.getUsername());
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 권한 없음 (일반 사용자가 다른 사용자 삭제 시도)")
    void deleteUser_forbidden() {
        // Given
        User anotherUser = User.builder().username("another").build();
        given(userRepository.findByUsernameAndDeletedAtIsNull(anotherUser.getUsername()))
                .willReturn(Optional.of(anotherUser));

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> userService.deleteUser(anotherUser.getUsername(), testUser));
        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
        assertThat(anotherUser.getDeletedAt()).isNull(); // Ensure not deleted
    }
}