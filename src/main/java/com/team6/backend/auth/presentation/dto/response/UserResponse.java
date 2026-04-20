package com.team6.backend.auth.presentation.dto.response;

import com.team6.backend.global.infrastructure.type.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String nickname;
    private Role role;

}
