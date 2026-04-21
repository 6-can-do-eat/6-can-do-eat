package com.team6.backend.order.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {
    @NotNull
    private UUID storeId;
    @NotNull
    private UUID addressId;
    @Size(max = 255)
    private String requestText;
    @NotEmpty
    @Valid
    private List<OrderItemCreateRequest> itemRequests;
}
