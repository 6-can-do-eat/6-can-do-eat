package com.team6.backend.payment.presetation.dto;

import com.team6.backend.order.presentation.dto.OrderResponse;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentResponse {
    private final UUID paymentId;
    private final UUID orderId;
    private final String paymentKey;
    private final String paymentType;
    private final PaymentStatus paymentStatus;
    private final Long amount;

    public static PaymentResponse from(Payment payment) {
        return new  PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentKey(),
                payment.getPaymentType(),
                payment.getStatus(),
                payment.getAmount()
        );
    }
}
