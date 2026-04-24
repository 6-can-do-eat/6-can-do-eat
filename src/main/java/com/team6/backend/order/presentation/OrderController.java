package com.team6.backend.order.presentation;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.order.application.OrderService;
import com.team6.backend.order.presentation.dto.OrderCreateRequest;
import com.team6.backend.order.presentation.dto.OrderResponse;
import com.team6.backend.user.domain.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<OrderResponse>> createOrder(@RequestBody @Valid OrderCreateRequest request,
                                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(SuccessResponse.created(orderService.createOrder(request, userId)));
    }

    @GetMapping("/orders")
    public ResponseEntity<SuccessResponse<Page<OrderResponse>>> getOrders(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10, sort = "createdBy", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(SuccessResponse.ok(orderService.getOrders(userId, pageable)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<SuccessResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(SuccessResponse.ok(orderService.getOrder(orderId)));
    }



}

