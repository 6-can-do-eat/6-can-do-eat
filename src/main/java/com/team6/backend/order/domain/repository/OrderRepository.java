package com.team6.backend.order.domain.repository;

import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.presentation.dto.OrderResponse;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndOrderStatus(UUID orderId, OrderStatus orderStatus);
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
    Page<Order> findAllByStore_User_Id(UUID userId, Pageable pageable);
}
