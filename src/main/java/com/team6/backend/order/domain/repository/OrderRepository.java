package com.team6.backend.order.domain.repository;

import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndStatus(UUID orderId, OrderStatus status);
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
    Page<Order> findAllByStore_OwnerId(UUID storeOwnerId, Pageable pageable);
}
