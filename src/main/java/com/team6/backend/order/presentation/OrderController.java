package com.team6.backend.order.presentation;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.order.application.OrderService;
import com.team6.backend.order.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "주문 생성")
    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<OrderResponse>> createOrder(@RequestBody @Valid OrderCreateRequest request,
                                                                     @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(SuccessResponse.created(orderService.createOrder(request, userId)));
    }

    @Operation(summary = "주문 목록 조회")
    @GetMapping("/orders")
    public ResponseEntity<SuccessResponse<Page<OrderResponse>>> getOrders(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10, sort = "createdBy", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(SuccessResponse.ok(orderService.getOrders(userId, securityUtils.getCurrentUserRole(), pageable)));
    }

    @Operation(summary = "주문 상세 조회")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<SuccessResponse<OrderResponse>> getOrder(@PathVariable UUID orderId,
                                                                   @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(SuccessResponse.ok(orderService.getOrder(orderId, userId, securityUtils.getCurrentUserRole())));
    }

    @Operation(summary = "주문 수정 (요청 사항)")
    @PutMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<OrderUpdate.Response>> updateOrder(@PathVariable UUID orderId,
                                                                      @RequestBody @Valid OrderUpdate.Request request) {
        return ResponseEntity.ok(SuccessResponse.ok(orderService.updateOrder(orderId, request)));
    }

    @Operation(summary = "주문 상태 변경")
    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    public ResponseEntity<SuccessResponse<OrderStatusUpdate.Response>> updateOrderStatus(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid OrderStatusUpdate.Request request
    ) {
        return ResponseEntity.ok(
                SuccessResponse.ok(orderService.updateOrderStatus(orderId, userId, securityUtils.getCurrentUserRole(), request))
        );
    }

    @Operation(summary = "주문 취소(5분 이내)")
    @PatchMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MASTER')")
    public ResponseEntity<SuccessResponse<OrderCancel.Response>> cancelOrder(
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(
                SuccessResponse.ok(orderService.cancelOrder(orderId))
        );
    }

    @Operation(summary = "주문 삭제(소프트 삭제)")
    @DeleteMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId, @AuthenticationPrincipal UUID userId) {
        orderService.deleteOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}

