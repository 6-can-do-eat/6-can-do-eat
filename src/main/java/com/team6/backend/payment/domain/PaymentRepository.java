package com.team6.backend.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Boolean existsByPaymentKey(String paymentKey);
}
