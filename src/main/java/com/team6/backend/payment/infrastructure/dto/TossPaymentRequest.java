package com.team6.backend.payment.infrastructure.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TossPaymentRequest {
    private String method;
    private Long amount;
    private String orderId;
    private String orderName;
    private String successUrl;
    private String failUrl;
}
