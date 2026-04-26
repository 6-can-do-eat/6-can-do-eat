package com.team6.backend.menu.domain.exception;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorCode {

    MENU_NOT_FOUND("MENU_404", HttpStatus.NOT_FOUND, "해당 메뉴가 존재하지 않습니다."),
    MENU_FORBIDDEN("MENU_403", HttpStatus.FORBIDDEN, "메뉴에 대한 권한이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

}
