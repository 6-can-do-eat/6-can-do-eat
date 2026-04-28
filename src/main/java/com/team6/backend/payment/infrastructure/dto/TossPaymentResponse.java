package com.team6.backend.payment.infrastructure.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossPaymentResponse {
    private String status;
    private Long amount;
    private Checkout checkout;

    @Getter
    @NoArgsConstructor
    public static class Checkout {
        private String url;
    }
}
