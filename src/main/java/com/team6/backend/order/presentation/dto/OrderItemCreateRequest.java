package com.team6.backend.order.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemCreateRequest {
    @NotNull
    private UUID menuId;
    @NotNull
    private Integer quantity;
}
