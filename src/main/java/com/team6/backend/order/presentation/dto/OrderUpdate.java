package com.team6.backend.order.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public final class OrderUpdate {
    private OrderUpdate() {}

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Request {

        @NotBlank
        @Size(max = 255)
        private String requestText;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private UUID orderId;
        private String requestText;

        public static Response from(UUID orderId, String requestText) {
            return new Response(orderId, requestText);
        }
    }
}
