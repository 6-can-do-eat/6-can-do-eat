package com.team6.backend.payment.presetation;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.payment.application.PaymentService;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import com.team6.backend.user.domain.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{orderId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<PaymentResponse>> confirmPayment(@PathVariable UUID orderId,
                                                                           @RequestBody @Valid PaymentConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(paymentService.confirmPayment(orderId, request)));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'MASTER')")
    public ResponseEntity<SuccessResponse<Page<PaymentResponse>>> getPayments(
            @PageableDefault(size = 10, sort = "createdBy", direction = Sort.Direction.DESC)
            Pageable pageable, Sort sort) {
        UUID userId = securityUtils.getCurrentUserId();
        Role role = securityUtils.getCurrentUserRole();
        return ResponseEntity.ok(SuccessResponse.ok(paymentService.getPayments(userId, role, pageable)));
    }
}
