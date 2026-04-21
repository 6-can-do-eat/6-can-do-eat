package com.team6.backend.auth.presentation.dto.response;

import com.team6.backend.user.domain.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String nickname;
    private Role role;

}
