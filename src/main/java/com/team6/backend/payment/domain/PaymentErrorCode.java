package com.team6.backend.payment.domain;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND("PAYMENT_404", HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_FORBIDDEN("PAYMENT_403", HttpStatus.FORBIDDEN, "결제에 대한 접근 권한이 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_400", HttpStatus.BAD_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_KEY_ALREADY_EXISTS("PAYMENT_409", HttpStatus.CONFLICT, "이미 처리된 결제 키입니다."),
    PAYMENT_INVALID_STATUS("PAYMENT_400", HttpStatus.BAD_REQUEST, "변경할 수 없는 결제 상태입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
