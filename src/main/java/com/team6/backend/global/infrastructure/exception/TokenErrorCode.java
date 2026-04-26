package com.team6.backend.global.infrastructure.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {

    INVALID_ACCESS_TOKEN("TOKEN_401", HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token입니다."),
    INVALID_REFRESH_TOKEN("TOKEN_401", HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_MISMATCH("TOKEN_401", HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
