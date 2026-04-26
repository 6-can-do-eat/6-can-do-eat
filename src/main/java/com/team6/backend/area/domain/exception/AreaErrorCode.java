package com.team6.backend.area.domain.exception;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AreaErrorCode implements ErrorCode {

    AREA_NOT_FOUND("AREA_404", HttpStatus.NOT_FOUND, "해당 지역을 찾을 수 없습니다."),
    AREA_FORBIDDEN("AREA_403", HttpStatus.FORBIDDEN, "지역에 대한 접근 권한이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}