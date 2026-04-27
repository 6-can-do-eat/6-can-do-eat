package com.team6.backend.user.domain.repository;

import com.team6.backend.global.infrastructure.config.AuditorConfig;
import com.team6.backend.global.infrastructure.config.JpaAuditingConfig;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, AuditorConfig.class})
class UserInfoRepositoryTest {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Test
    @DisplayName("사용자 이름으로 조회 성공")
    void findByUsername_Success() {
        // given
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .role(Role.CUSTOMER)
                .nickname("테스터")
                .build();
        userInfoRepository.save(user);

        // when
        Optional<User> foundUser = userInfoRepository.findByUsername("testuser");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("삭제되지 않은 사용자만 조회 - 성공")
    void findByUsernameAndDeletedAtIsNull_Success() {
        // given
        User activeUser = User.builder()
                .username("active_user")
                .password("password")
                .role(Role.CUSTOMER)
                .nickname("활동중")
                .build();
        userInfoRepository.save(activeUser);

        // when
        Optional<User> result = userInfoRepository.findByUsernameAndDeletedAtIsNull("active_user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("삭제된 사용자는 조회되지 않아야 함")
    void findByUsernameAndDeletedAtIsNull_DeletedUser() {
        // given
        User deletedUser = User.builder()
                .username("deleted_user")
                .password("password")
                .role(Role.CUSTOMER)
                .nickname("삭제됨")
                .build();
        userInfoRepository.save(deletedUser);
        
        // Soft delete 수행
        deletedUser.markDeleted("system");
        userInfoRepository.saveAndFlush(deletedUser);

        // when
        Optional<User> result = userInfoRepository.findByUsernameAndDeletedAtIsNull("deleted_user");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유저네임 또는 닉네임 검색 테스트")
    void searchUser_Success() {
        // given
        User user1 = User.builder()
                .username("sparta_kim")
                .password("password")
                .role(Role.CUSTOMER)
                .nickname("김철수")
                .build();
        User user2 = User.builder()
                .username("abc_lee")
                .password("password")
                .role(Role.CUSTOMER)
                .nickname("sparta_king")
                .build();
        userInfoRepository.save(user1);
        userInfoRepository.save(user2);

        // when - "sparta"가 포함된 유저네임 또는 닉네임 검색
        Page<User> result = userInfoRepository.findAllByDeletedAtIsNullAndUsernameContainingOrNicknameContainingAndDeletedAtIsNull(
                "sparta", "sparta", PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
