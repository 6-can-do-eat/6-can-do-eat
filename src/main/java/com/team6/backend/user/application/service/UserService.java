package com.team6.backend.user.application.service;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.userInfoRepository;
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

    private final userInfoRepository userRepository;
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
        validateOwnership(username, currentUser);
        User targetUser = getUserByUsername(username);

        targetUser.updateUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            targetUser.updatePassword(passwordEncoder.encode(request.getPassword()));
        }

        return UserInfoResponse.from(targetUser);
    }

    /**
     * 사용자 권한 변경 (MASTER 전용)
     * TODO 그아웃(토큰 삭제)을 하지 않는 대신,
     * 시스템의 다른 부분에서 이 사용자의 최신 Role을 DB에서 조회하도록 강제해야 함
     */
    @Transactional
    public UserInfoResponse updateUserRole(String username, Role newRole, User currentUser) {
        User targetUser = getUserByUsername(username);

        if (targetUser.getRole().equals(newRole)) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        if (targetUser.getUsername().equals(currentUser.getUsername())) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        // 권한 업데이트 및 즉시 DB 반영
        targetUser.updateRole(newRole);
        userRepository.saveAndFlush(targetUser);

        // 2. Redis에서 해당 사용자의 Refresh Token 삭제 (로그아웃 효과)
        // 이를 통해 기존 Access Token 만료 시 재발급을 차단하여 강제 재인증 유도
        // 참고: 만약 Access Token까지 즉시 무효화하려면 컨트롤러에서 토큰 문자열을 넘겨받아
        // tokenService.blacklistAccessToken(token, "ROLE_CHANGED") 를 호출해야 함.
        //tokenService.deleteRefreshToken(targetUser.getId());.


        return UserInfoResponse.from(targetUser);
    }

    /**
     * TODO 메뉴를 수정할 때 컨트롤러에서 넘겨받은 user 객체의 권한만 믿지 말고, userService를 호출하여 실시간 상태를 체크합니다.
     * [실시간 권한 검증 유틸]
     *  * 토큰의 권한이 아닌, DB의 최신 권한을 기준으로 체크합니다.
     *  * @param userId 체크할 사용자 ID
     *  * @param requiredRole 필요한 권한 (예: Role.OWNER)
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
        User targetUser = getUserByUsername(username);
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
