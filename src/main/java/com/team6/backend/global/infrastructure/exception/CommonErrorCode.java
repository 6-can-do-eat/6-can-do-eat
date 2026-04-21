package com.team6.backend.global.infrastructure.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE("COMMON_400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("COMMON_401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("COMMON_403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND("COMMON_404", HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    CONFLICT("COMMON_409", HttpStatus.CONFLICT, "충돌이 발생했습니다."),

    INTERNAL_SERVER_ERROR("COMMON_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
