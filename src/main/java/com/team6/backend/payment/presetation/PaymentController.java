package com.team6.backend.payment.presetation;

import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.payment.application.PaymentService;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<PaymentResponse>> confirmPayment(@PathVariable UUID orderId,
                                                                           @RequestBody @Valid PaymentConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(paymentService.confirmPayment(orderId, request)));
    }
}
