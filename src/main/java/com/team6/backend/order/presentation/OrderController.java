package com.team6.backend.order.presentation;

import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.order.application.OrderService;
import com.team6.backend.order.presentation.dto.OrderCreateRequest;
import com.team6.backend.order.presentation.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<OrderResponse>> createOrder(@RequestBody @Valid OrderCreateRequest request,
                                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(SuccessResponse.created(orderService.createOrder(request, userId)));
    }



}
