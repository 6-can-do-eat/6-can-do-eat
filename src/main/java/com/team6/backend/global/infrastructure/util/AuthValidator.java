package com.team6.backend.global.infrastructure.util;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.ErrorCode;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthValidator {

    private final SecurityUtils securityUtils;

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
        UUID currentUserId = securityUtils.getCurrentUserId();

        // 무조건 허용 권한 목록에 포함되어 있는지 확인
        if (alwaysAllowed != null && alwaysAllowed.contains(userRole)) {
            return;
        }

        // 조건부 허용 권한 목록에 포함되어 있다면 소유권 일치 여부 확인
        if (conditionallyAllowed != null && conditionallyAllowed.contains(userRole)) {
            if (currentUserId.equals(resourceOwnerId)) {
                return;
            }
        }

        // 이외의 모든 경우는 예외 처리
        log.warn("[AUTH_VALIDATOR] 권한 검증 실패 userId: {}, userRole: {}, resourceOwnerId: {}, errorCode: {}",
                currentUserId, userRole, resourceOwnerId, errorCode.getCode());
        throw new ApplicationException(errorCode);
    }
}
