package com.team6.backend.payment.infrastructure.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TossPaymentConfirmResponse {
    private String paymentKey;
    private String orderId;
    private Long totalAmount;
    private String status;
    private String method;
    private String approvedAt;
}
