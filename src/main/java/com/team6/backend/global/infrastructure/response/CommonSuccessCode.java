package com.team6.backend.global.infrastructure.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonSuccessCode implements SuccessCode {

    OK("COMMON_200", HttpStatus.OK, "SUCCESS"),
    CREATED("COMMON_201", HttpStatus.CREATED, "CREATED"),
    ACCEPTED("COMMON_202", HttpStatus.ACCEPTED, "ACCEPTED"),
    NO_CONTENT("COMMON_204", HttpStatus.NO_CONTENT, "NO_CONTENT");

    private final String code;
    private final HttpStatus status;
    private final String message;
}