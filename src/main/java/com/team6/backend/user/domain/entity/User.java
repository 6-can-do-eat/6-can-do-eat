package com.team6.backend.user.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
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

    public User(String username, String password, Role role, String nickname) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.nickname = nickname;
    }

}
