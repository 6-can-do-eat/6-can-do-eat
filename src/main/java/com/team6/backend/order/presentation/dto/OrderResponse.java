package com.team6.backend.order.presentation.dto;

import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.entity.OrderItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderResponse {
    private final UUID orderId;
    private final UUID userId;
    private final UUID storeId;
    private final UUID addressId;
    private final Long totalPrice;
    private final String requestText;
    private final List<OrderItemResponse> items;

    public static OrderResponse from(Order order, UUID userId, List<OrderItem> orderItems) {
        return new OrderResponse(
                order.getId(),
                userId,
                order.getStore().getStoreId(),
                order.getAddress().getAdId(),
                order.getTotalPrice(),
                order.getRequestText(),
                orderItems.stream()
                        .map(OrderItemResponse::from)
                        .toList());
    }
}
