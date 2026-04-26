package com.team6.backend.auth.domain.repository;

import com.team6.backend.global.infrastructure.config.AuditorConfig;
import com.team6.backend.global.infrastructure.config.JpaAuditingConfig;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, AuditorConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(new User("user1", "encodedPassword", Role.CUSTOMER, "닉네임1"));
    }

    @Test
    @DisplayName("username으로 유저 조회 성공")
    void findByUsername_success() {
        // when
        Optional<User> result = userRepository.findByUsername("user1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("username으로 유저 조회 실패 - 존재하지 않는 username")
    void findByUsername_fail() {
        // when
        Optional<User> result = userRepository.findByUsername("notExist");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("username 중복 체크 - 존재하는 경우 true")
    void existsByUsername_true() {
        // when
        boolean result = userRepository.existsByUsername("user1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("username 중복 체크 - 존재하지 않는 경우 false")
    void existsByUsername_false() {
        // when
        boolean result = userRepository.existsByUsername("notExist");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("nickname 중복 체크 - 존재하는 경우 true")
    void existsByNickname_true() {
        // when
        boolean result = userRepository.existsByNickname("닉네임1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("nickname 중복 체크 - 존재하지 않는 경우 false")
    void existsByNickname_false() {
        // when
        boolean result = userRepository.existsByNickname("없는닉네임");

        // then
        assertThat(result).isFalse();
    }
}