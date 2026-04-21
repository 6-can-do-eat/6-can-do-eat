package com.team6.backend.order.presentation.dto;

import com.team6.backend.order.domain.entity.OrderItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderItemCreateResponse {
    private final UUID orderItemId;
    private final UUID orderId;
    private final UUID menuId;
    private final Integer quantity;
    private final Integer unitPrice;

    public static OrderItemCreateResponse from(OrderItem orderItem) {
        return OrderItemCreateResponse.builder()
                .orderItemId(orderItem.getId())
                .orderId(orderItem.getOrder().getId())
                .menuId(orderItem.getMenu().getId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .build();
    }
}
