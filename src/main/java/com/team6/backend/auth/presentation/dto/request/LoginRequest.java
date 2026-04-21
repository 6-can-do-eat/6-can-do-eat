package com.team6.backend.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank(message = "username은 필수입니다.")
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    private String password;

}
