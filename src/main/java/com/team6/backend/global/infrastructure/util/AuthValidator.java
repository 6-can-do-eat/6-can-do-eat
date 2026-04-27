package com.team6.backend.global.infrastructure.util;

import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.AuthErrorCode;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.exception.ErrorCode;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthValidator {

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    /**
     * 권한 및 소유권 검증 메서드
     * @param resourceOwnerId 리소스의 소유자 ID (해당 없는 경우 null)
     * @param alwaysAllowed 무조건 통과시킬 권한 목록 (예: MASTER)
     * @param conditionallyAllowed 현재 사용자가 리소스 소유자일 때만 통과시킬 권한 목록 (예: OWNER, CUSTOMER)
     * @param errorCode 권한이 없을 때 발생시킬 예외 코드
     */
    public void validateAccess(
            UUID resourceOwnerId,
            List<Role> alwaysAllowed,
            List<Role> conditionallyAllowed,
            ErrorCode errorCode
    ) {

        Role userRole = securityUtils.getCurrentUserRole();

        // 매 요청 시 DB 권한 재검증 (토큰 내 role과 실제 DB role 비교)
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new ApplicationException(AuthErrorCode.USER_NOT_FOUND));
        if (!user.getRole().equals(userRole)) throw new ApplicationException(CommonErrorCode.CONFLICT);

        // 무조건 허용 권한 목록에 포함되어 있는지 확인
        if (alwaysAllowed != null && alwaysAllowed.contains(userRole)) {
            return;
        }

        // 조건부 허용 권한 목록에 포함되어 있다면 소유권 일치 여부 확인
        if (conditionallyAllowed != null && conditionallyAllowed.contains(userRole)) {
            UUID currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId.equals(resourceOwnerId)) {
                return;
            }
        }

        // 이외의 모든 경우는 예외 처리
        throw new ApplicationException(errorCode);
    }
}
