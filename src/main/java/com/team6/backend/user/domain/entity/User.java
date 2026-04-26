package com.team6.backend.user.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "p_user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Role role;

    @Column(length = 100)
    private String nickname;

    // 닉네임 수정
    public void updateUsername(String username) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
    }

    // 비밀번호 수정
    public void updatePassword(String encodedPassword) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
    }

    // 권한 수정
    public void updateRole(Role role) {
        if (role != null) {
            this.role = role;
        }
    }
}
