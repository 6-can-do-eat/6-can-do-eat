package com.team6.backend.payment.presentation;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.payment.application.PaymentService;
import com.team6.backend.payment.infrastructure.dto.TossPaymentResponse;
import com.team6.backend.payment.presentation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presentation.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "Payment", description = "결제 관리 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "결제 승인")
    @PostMapping("/orders/{orderId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<PaymentResponse>> confirmPayment(@PathVariable UUID orderId,
                                                                           @RequestBody @Valid PaymentConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(paymentService.confirmPayment(orderId, request)));
    }

    @Operation(summary = "결제 내역 조회")
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<SuccessResponse<Page<PaymentResponse>>> getPayments(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10, sort = "createdBy", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(SuccessResponse.ok(paymentService.getPayments(
                userId,
                securityUtils.getCurrentUserRole(),
                pageable)));
    }

    @Operation(summary = "결제 내역 상세 조회")
    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<SuccessResponse<PaymentResponse>> getPayment(@PathVariable UUID paymentId,
                                                                       @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(SuccessResponse.ok(paymentService.getPayment(paymentId, userId, securityUtils.getCurrentUserRole())));
    }

    @Operation(summary = "결제 삭제 (소프트 삭제)")
    @DeleteMapping("/payments/{paymentId}")
    @PreAuthorize("hasAnyRole('MASTER')")
    public ResponseEntity<SuccessResponse<?>> deletePayment(@PathVariable UUID paymentId) {
        paymentService.deletePayment(paymentId, securityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /* ================== Toss PG 연동 결제창 생성  ======================= */

    @PostMapping("/orders/payments/toss-checkout")
    public ResponseEntity<TossPaymentResponse> createCheckout() {
        return ResponseEntity.ok(paymentService.createCheckout());
    }

    @GetMapping("/toss/success")
    public ResponseEntity<Map<String, String>> tossSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount
    ) {
        return ResponseEntity.ok(Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount.toString()
        ));
    }

    @GetMapping("/toss/fail")
    public ResponseEntity<Map<String, String>> tossFail(
            @RequestParam String code,
            @RequestParam String message
    ) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", code,
                "message", message
        ));
    }
}
