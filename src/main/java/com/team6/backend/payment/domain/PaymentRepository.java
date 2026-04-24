package com.team6.backend.payment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Boolean existsByPaymentKey(String paymentKey);
    Page<Payment> findAll(Pageable pageable);
    Page<Payment> findAllByOrder_User_Id(UUID userId, Pageable pageable);
}
