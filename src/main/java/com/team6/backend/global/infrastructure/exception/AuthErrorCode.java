package com.team6.backend.global.infrastructure.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_USERNAME("AUTH_409", HttpStatus.CONFLICT, "이미 존재하는 아이디입니다."),
    DUPLICATE_NICKNAME("AUTH_409", HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    INVALID_CREDENTIALS("AUTH_401", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_ROLE("AUTH_400", HttpStatus.BAD_REQUEST, "허용되지 않은 권한입니다."),
    USER_NOT_FOUND("AUTH_404", HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
