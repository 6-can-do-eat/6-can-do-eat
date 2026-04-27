package com.team6.backend.user.application.service;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.UserInfoRepository;
import com.team6.backend.user.presentation.dto.request.UserInfoRequest;
import com.team6.backend.user.presentation.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 상세 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserDetail(String username) {
        return UserInfoResponse.from(getUserByUsername(username));
    }

    /**
     * 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<UserInfoResponse> getUsers(String keyword, int page, int size, String sortBy, boolean isAsc) {
        Sort sort = isAsc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;
        if (keyword != null && !keyword.isBlank()) {
            userPage = userRepository.findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                    keyword, keyword, pageable);
        } else {
            userPage = userRepository.findAllByDeletedAtIsNull(pageable);
        }

        return userPage.map(UserInfoResponse::from);
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserInfoResponse updateUser(String username, UserInfoRequest request, User currentUser) {
        // 1. 권한 체크 (본인 또는 MASTER)
        validateOwnership(username, currentUser);

        // 2. 대상 조회
        User targetUser = getUserByUsername(username);

        // 3. 수정
        targetUser.updateUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            targetUser.updatePassword(passwordEncoder.encode(request.getPassword()));
        }

        return UserInfoResponse.from(targetUser);
    }

    /**
     * 사용자 권한 변경 (MASTER 전용)
     */
    @Transactional
    public UserInfoResponse updateUserRole(String username, Role newRole, User currentUser) {
        // 1. MASTER 권한 체크
        if (!currentUser.getRole().equals(Role.MASTER)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        // 2. 대상 조회
        User targetUser = getUserByUsername(username);

        // 3. 동일 권한 변경 체크
        if (targetUser.getRole().equals(newRole)) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        // 4. 본인 권한 변경 금지
        if (targetUser.getUsername().equals(currentUser.getUsername())) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        // 5. 업데이트
        targetUser.updateRole(newRole);
        userRepository.saveAndFlush(targetUser);

        return UserInfoResponse.from(targetUser);
    }

    /**
     * 실시간 권한 검증 유틸
     */
    @Transactional(readOnly = true)
    public void validateUserRole(UUID userId, Role requiredRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (!user.getRole().equals(requiredRole)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
    }

    /**
     * 사용자 삭제(소프트)
     */
    @Transactional
    public void deleteUser(String username, User currentUser) {
        // 1. 권한 체크 (본인 또는 MASTER)
        validateOwnership(username, currentUser);

        // 2. 대상 조회
        User targetUser = getUserByUsername(username);

        // 3. 삭제 처리
        targetUser.markDeleted(currentUser.getUsername());
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateOwnership(String username, User currentUser) {
        if (!currentUser.getRole().equals(Role.MASTER) && !currentUser.getUsername().equals(username)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
    }
}
