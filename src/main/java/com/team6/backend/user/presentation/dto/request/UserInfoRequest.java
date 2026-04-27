package com.team6.backend.user.presentation.dto.request;

import com.team6.backend.user.domain.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserInfoRequest {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해 주세요.")
    private String username;

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    // 역할 수정이 필요한 경우 (보통 관리자용)
    private Role role;

    public UserInfoRequest(String username, String password) {
        this.username = username;
        this.password = password;

    }

}
