package com.team6.backend.user.presentation.dto.response;

import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {

    private UUID userId;
    private String username;
    private String nickname;
    private Role role;
    private LocalDateTime createdAt;

    /**
     * User 엔티티를 받아 Response DTO로 변환하는 생성자
     */
    public UserInfoResponse(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
    }

    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
