package com.team6.backend.order.domain;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("ORDER_404", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_FORBIDDEN("ORDER_403", HttpStatus.FORBIDDEN, "주문에 대한 접근 권한이 없습니다."),
    ORDER_INVALID_STATUS("ORDER_400", HttpStatus.BAD_REQUEST, "변경할 수 없는 주문 상태입니다."),
    STORE_NOT_ORDERABLE("ORDER_400", HttpStatus.BAD_REQUEST, "주문할 수 없는 가게입니다."),
    MENU_NOT_ORDERABLE("ORDER_400", HttpStatus.BAD_REQUEST, "주문할 수 없는 메뉴입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
