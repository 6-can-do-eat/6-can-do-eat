package com.team6.backend.order.domain.repository;

import com.team6.backend.order.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findAllByOrder_IdIn(List<UUID> orderIds);
}
