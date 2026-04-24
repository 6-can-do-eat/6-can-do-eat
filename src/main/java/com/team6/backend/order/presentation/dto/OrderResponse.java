package com.team6.backend.order.presentation.dto;

import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.entity.OrderItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
    private final Integer totalPrice;
    private final String requestText;
    private final List<OrderItemResponse> items;

    /*
    public static OrderResponse from(Order order, UUID userId, List<OrderItem> orderItems) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(userId)
                .storeId(order.getStore().getId())
                .addressId(order.getAddress().getId())
                .totalPrice(order.getTotalPrice())
                .requestText(order.getRequestText())
                .items(orderItems.stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
     */
}
