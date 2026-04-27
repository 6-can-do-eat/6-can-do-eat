package com.team6.backend.global.infrastructure.util;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthValidatorTest {

    @InjectMocks
    private AuthValidator authValidator;

    @Mock
    private SecurityUtils securityUtils;

    @Test
    @DisplayName("무조건 허용 권한(예: MASTER)에 포함된 사용자는 검증을 통과한다")
    void validateAccess_Success_WhenRoleIsAlwaysAllowed() {
        // given: 현재 사용자의 권한이 MASTER인 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);

        // when & then: 예외 없이 통과해야 함
        assertDoesNotThrow(() -> authValidator.validateAccess(
                UUID.randomUUID(),
                List.of(Role.MASTER),
                List.of(Role.OWNER, Role.CUSTOMER),
                CommonErrorCode.FORBIDDEN
        ));
    }

    @Test
    @DisplayName("조건부 허용 권한(예: OWNER)이고 리소스 소유자 ID가 일치하면 검증을 통과한다")
    void validateAccess_Success_WhenRoleIsConditionallyAllowedAndIsOwner() {
        // given: 현재 사용자의 ID와 리소스 소유자 ID가 동일한 상황
        UUID userId = UUID.randomUUID();
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(userId);

        // when & then: 소유권이 일치하므로 통과해야 함
        assertDoesNotThrow(() -> authValidator.validateAccess(
                userId,
                List.of(Role.MASTER),
                List.of(Role.OWNER),
                CommonErrorCode.FORBIDDEN
        ));
    }

    @Test
    @DisplayName("조건부 허용 권한이지만 리소스 소유자 ID가 일치하지 않으면 예외가 발생한다")
    void validateAccess_Fail_WhenRoleIsConditionallyAllowedAndIsNotOwner() {
        // given: 현재 사용자의 ID와 리소스 소유자 ID가 다른 상황
        UUID currentUserId = UUID.randomUUID();
        UUID resourceOwnerId = UUID.randomUUID();

        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(currentUserId);

        // when: 검증 실행 시 ApplicationException 발생 확인
        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        resourceOwnerId,
                        List.of(Role.MASTER),
                        List.of(Role.OWNER),
                        CommonErrorCode.FORBIDDEN
                )
        );

        // then: 설정한 에러 코드(FORBIDDEN)가 포함되어야 함
        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허용 목록에 없는 권한을 가진 사용자가 접근하면 예외가 발생한다")
    void validateAccess_Fail_WhenRoleIsNotInAnyList() {
        // given: 현재 사용자가 CUSTOMER 권한을 가졌으나, 허용 목록에는 없는 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when: 검증 실행
        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        UUID.randomUUID(),
                        List.of(Role.MASTER),
                        List.of(Role.OWNER),
                        CommonErrorCode.FORBIDDEN
                )
        );

        // then: 접근 권한 없음 에러 발생
        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허용 목록이 null인 경우에도 권한이 없으면 예외가 발생한다")
    void validateAccess_HandleNullLists() {
        // given: 허용 목록이 비어있는 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when & then
        assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        UUID.randomUUID(),
                        null,
                        null,
                        CommonErrorCode.FORBIDDEN
                )
        );
    }

    @Test
    @DisplayName("리소스 소유자 ID가 null인 경우, 조건부 권한 사용자는 예외가 발생한다")
    void validateAccess_Fail_WhenResourceOwnerIdIsNull() {
        // given: 현재 사용자는 OWNER 권한이지만, 비교할 리소스 소유자 ID가 없는 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(UUID.randomUUID());

        // when & then: null과 비교하게 되므로 실패해야 함
        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        null,
                        List.of(Role.MASTER),
                        List.of(Role.OWNER),
                        CommonErrorCode.FORBIDDEN
                )
        );
        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("허용 권한 목록들이 모두 빈 리스트일 때 접근하면 예외가 발생한다")
    void validateAccess_Fail_WhenBothListsAreEmpty() {
        // given: 모든 사용자에 대해 권한을 막아둔 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);

        // when & then: 목록이 비어있으면 어떤 권한도 통과할 수 없음
        assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        UUID.randomUUID(),
                        List.of(),
                        List.of(),
                        CommonErrorCode.FORBIDDEN
                )
        );
    }

    @Test
    @DisplayName("상황에 따라 다른 에러 코드가 전달되면 해당 에러 코드를 포함한 예외를 던진다")
    void validateAccess_Fail_WithCustomErrorCode() {
        // given: 권한이 없는 사용자가 접근할 때 RESOURCE_NOT_FOUND 에러를 기대하는 상황
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when & then: 요청 시 전달한 ErrorCode가 예외에 담겨야 함
        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        UUID.randomUUID(),
                        List.of(Role.MASTER),
                        List.of(Role.OWNER),
                        CommonErrorCode.RESOURCE_NOT_FOUND
                )
        );
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("SecurityUtils에서 인증 실패 예외가 발생하면 Validator도 이를 전파한다")
    void validateAccess_Fail_WhenSecurityUtilsThrowsException() {
        // given: SecurityUtils 단계에서 이미 인증 에러가 발생하는 상황
        given(securityUtils.getCurrentUserRole()).willThrow(new ApplicationException(CommonErrorCode.UNAUTHORIZED));

        // when & then: Validator 내부에서 발생한 예외가 그대로 밖으로 던져지는지 확인
        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                authValidator.validateAccess(
                        UUID.randomUUID(),
                        List.of(Role.MASTER),
                        null,
                        CommonErrorCode.FORBIDDEN
                )
        );
        assertEquals(CommonErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 권한이 무조건 허용과 조건부 허용 양쪽에 모두 포함되어 있어도 정상 통과한다")
    void validateAccess_Success_WhenRoleInBothLists() {
        // given: MASTER 권한이 두 리스트에 모두 포함된 특이 케이스
        UUID userId = UUID.randomUUID();
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MASTER);

        // when & then: 첫 번째 check(alwaysAllowed)에서 이미 return 되므로 소유권 확인 없이 통과해야 함
        assertDoesNotThrow(() -> authValidator.validateAccess(
                UUID.randomUUID(), // 소유자가 아니더라도 MASTER이므로 통과
                List.of(Role.MASTER),
                List.of(Role.MASTER),
                CommonErrorCode.FORBIDDEN
        ));
    }
}