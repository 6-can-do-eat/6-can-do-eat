package com.team6.backend.global.infrastructure.config.security.util;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.type.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UUID getCurrentUserId() {
        return (UUID) getAuthentication().getPrincipal();
    }

    public Role getCurrentUserRole() {
        return getAuthentication().getAuthorities()
                .stream()
                .findFirst()
                .map(a -> {
                    String authority = a.getAuthority();
                    if (authority == null) {
                        throw new ApplicationException(CommonErrorCode.UNAUTHORIZED);
                    }
                    return Role.valueOf(authority.replace("ROLE_", ""));
                })
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.UNAUTHORIZED));
    }

    private Authentication getAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException(CommonErrorCode.UNAUTHORIZED);
        }
        return authentication;
    }
}
