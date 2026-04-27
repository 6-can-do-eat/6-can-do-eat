package com.team6.backend.order.domain.repository;

import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndStatus(UUID orderId, OrderStatus status);
    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
    Page<Order> findAllByStore_OwnerId(UUID storeOwnerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findByIdForUpdate(UUID orderId);
}
