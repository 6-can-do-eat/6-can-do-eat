package com.team6.backend.payment.domain;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.order.domain.entity.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(unique = true, nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private String paymentType = "CARD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private Long amount;

    public static Payment createPayment(Order order, String paymentKey, Long amount) {
        Payment payment = new Payment();
        payment.order = order;
        payment.paymentKey = paymentKey;
        payment.amount = amount;
        return payment;
    }

    public void updatePaymentStatus(PaymentStatus paymentStatus) {
        if (!this.status.canChangeTo(paymentStatus)) {
            throw new ApplicationException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        this.status = paymentStatus;
    }
}
