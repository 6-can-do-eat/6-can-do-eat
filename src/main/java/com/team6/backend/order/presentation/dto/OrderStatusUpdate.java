package com.team6.backend.order.presentation.dto;

import com.team6.backend.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public final class OrderStatusUpdate {

    private OrderStatusUpdate() {
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @NotNull
        private OrderStatus orderStatus;
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
