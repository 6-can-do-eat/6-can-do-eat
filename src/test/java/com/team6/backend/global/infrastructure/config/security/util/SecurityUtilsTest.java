package com.team6.backend.global.infrastructure.config.security.util;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.user.domain.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        securityUtils = new SecurityUtils();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(UUID userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void shouldReturnUserId_whenAuthenticated() {
        UUID userId = UUID.randomUUID();
        setAuthentication(userId, "CUSTOMER");

        assertThat(securityUtils.getCurrentUserId()).isEqualTo(userId);
    }

    @Test
    void shouldReturnRole_whenAuthenticated() {
        setAuthentication(UUID.randomUUID(), "OWNER");

        assertThat(securityUtils.getCurrentUserRole()).isEqualTo(Role.OWNER);
    }

    @Test
    void shouldThrowException_whenNoAuthentication() {
        assertThatThrownBy(() -> securityUtils.getCurrentUserId())
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void shouldThrowClassCastException_whenPrincipalIsNotUUID() {
        var auth = new UsernamePasswordAuthenticationToken(
                "not-uuid",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> securityUtils.getCurrentUserId())
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void shouldThrowException_whenNoAuthorities() {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(),
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> securityUtils.getCurrentUserRole())
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void shouldReturnRole_whenPrefixIsMissing() {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(),
                null,
                List.of(new SimpleGrantedAuthority("OWNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(securityUtils.getCurrentUserRole()).isEqualTo(Role.OWNER);
    }

}