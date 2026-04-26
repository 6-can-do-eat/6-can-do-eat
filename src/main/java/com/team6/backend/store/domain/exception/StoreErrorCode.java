package com.team6.backend.store.domain.exception;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements ErrorCode {

    STORE_NOT_FOUND("STORE_404", HttpStatus.NOT_FOUND, "해당 가게가 존재하지 않습니다."),
    STORE_FORBIDDEN("STORE_403", HttpStatus.FORBIDDEN, "가게에 대한 권한이 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

}
