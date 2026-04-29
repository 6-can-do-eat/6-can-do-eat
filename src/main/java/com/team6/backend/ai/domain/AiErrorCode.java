package com.team6.backend.ai.domain;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

    AI_PROMPT_REQUIRED("AI_400", HttpStatus.BAD_REQUEST, "프롬프트는 필수입니다."),
    AI_PROMPT_TOO_LONG("AI_400", HttpStatus.BAD_REQUEST, "프롬프트는 최대 100자까지 입력할 수 있습니다."),
    AI_API_KEY_MISSING("AI_500", HttpStatus.INTERNAL_SERVER_ERROR, "AI API 키가 설정되어 있지 않습니다."),
    AI_REQUEST_FAILED("AI_500", HttpStatus.INTERNAL_SERVER_ERROR, "AI 요청 처리 중 오류가 발생했습니다."),
    AI_EMPTY_RESPONSE("AI_502", HttpStatus.BAD_GATEWAY, "AI 응답이 비어 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
