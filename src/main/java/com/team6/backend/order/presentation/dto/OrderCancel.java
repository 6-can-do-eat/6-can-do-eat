package com.team6.backend.order.presentation.dto;

import com.team6.backend.order.domain.OrderStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

public final class OrderCancel {

    private OrderCancel() {
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private UUID orderId;
        private OrderStatus orderStatus;

        public static Response from(UUID orderId, OrderStatus orderStatus) {
            return new Response(orderId, orderStatus);
        }
    }
}