package com.team6.backend.payment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentKey(String paymentKey);

    Page<Payment> findAll(Pageable pageable);
    Page<Payment> findAllByOrder_User_Id(UUID userId, Pageable pageable);

    @Modifying
    @Query(value = """
        insert into p_payment (
            id,
            order_id,
            payment_key,
            payment_type,
            status,
            amount,
            created_at,
            created_by,
            updated_at,
            updated_by
        )
        values (
            :paymentId,
            :orderId,
            :paymentKey,
            'CARD',
            'COMPLETED',
            :amount,
            current_timestamp,
            :auditor,
            current_timestamp,
            :auditor
        )
        on conflict do nothing
        """, nativeQuery = true)
    int insertPaymentIfAbsent(
            UUID paymentId,
            UUID orderId,
            String paymentKey,
            Long amount,
            String auditor
    );
}
