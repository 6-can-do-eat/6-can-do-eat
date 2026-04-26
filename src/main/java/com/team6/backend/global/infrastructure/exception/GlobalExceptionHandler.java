package com.team6.backend.global.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /* 전체 커스텀 예외 처리 */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(e.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * *@Valid 요청 데이터 검증 시 발생하는 예외처리
     * 컨트롤러에서  @RequestBody + @Valid를 사용하는경우 Dto 검증 조건에 맞지않으면
     * MethodArgumentNotValidException이 발생
     * 이 핸들러는 위 예외 에서 BindingResult 로 에러를 가져오고, 첫 메시지를 클라이언트에게 반환하도록 구현
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        log.warn(e.getMessage());

        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return  ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message));
    }

    /**
     * 서비스 로직 예외처리
     * 잘못된 요청이나 조건에 맞지 않는경우
     * IllegalArgumentException을 throw하면 이 핸들러에서 처리함
     * 예외 메시지는 서비스에서 정의
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegal(IllegalArgumentException e) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        log.warn(e.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    /**
     * 컨트롤러 권한 없음 예외처리
     * 컨트롤러에서 @PreAuthorize를 사용하는 경우 설정한 권한 외의 사용자가 접근하면
     * AccessDeniedException이 발생
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = CommonErrorCode.FORBIDDEN;
        log.error(e.getMessage(), e);

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 경로 변수나 쿼리 파라미터의 타입이 일치하지 않을 때
     * MethodArgumentTypeMismatchException이 발생
     * (예: UUID 자리에 일반 문자열)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        log.warn("[TYPE_MISMATCH] field: {}, value: {}", e.getName(), e.getValue());

        String message = String.format("파라미터 '%s'의 형식이 올바르지 않습니다. (입력값: %s)", e.getName(), e.getValue());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, message));
    }

    /* Fallback 예외 처리 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        log.error(e.getMessage(), e);

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /* JSON 파싱 실패 예외 처리 (잘못된 형식 또는 허용되지 않은 Enum 값) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        log.warn(e.getMessage());

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, "올바르지 않은 요청 형식입니다."));
    }
}
