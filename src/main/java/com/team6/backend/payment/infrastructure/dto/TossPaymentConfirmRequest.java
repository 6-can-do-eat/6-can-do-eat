package com.team6.backend.payment.infrastructure.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TossPaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private Long amount;
}
