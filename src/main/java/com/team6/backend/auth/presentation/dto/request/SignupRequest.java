package com.team6.backend.auth.presentation.dto.request;

import com.team6.backend.user.domain.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SignupRequest {

    @NotBlank(message = "username은 필수입니다.")
    @Pattern(
            regexp = "^[a-z0-9]{4,10}$",
            message = "username은 4~10자 소문자+숫자만 가능합니다."
    )
    private String username;

    @NotBlank(message = "비밀번호은 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,15}$",
            message = "비밀번호는 8~15자 영문+숫자+특수문자 포함이어야 합니다."
    )
    private String password;

    @NotNull(message = "role은 필수입니다.")
    private Role role;

    private String nickname;

}
