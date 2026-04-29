package com.team6.backend.order.application;

import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.entity.OrderItem;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.presentation.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCreateService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // OrderService에서 메서드로 구현 시 프록시 적용 X
    @Transactional
    public OrderResponse save(Order order, List<OrderItem> orderItems, UUID userId) {
        orderRepository.saveAndFlush(order);
        orderItemRepository.saveAll(orderItems);
        return OrderResponse.from(order, userId, orderItems);
    }
}
