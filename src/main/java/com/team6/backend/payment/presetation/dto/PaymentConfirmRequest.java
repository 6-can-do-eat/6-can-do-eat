package com.team6.backend.payment.presetation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {
    @NotEmpty
    private String paymentKey;

    @NotEmpty
    private String orderId;

    @NotNull
    private Long amount;
}
