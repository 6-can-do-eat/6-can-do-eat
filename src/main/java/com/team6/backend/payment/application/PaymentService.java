package com.team6.backend.payment.application;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentRepository;
import com.team6.backend.payment.domain.PaymentStatus;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentResponse confirmPayment(UUID orderId, PaymentConfirmRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        // 결제 금액 일치 여부
        if (request.getAmount() != order.getTotalPrice()) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
        // 중복 paymentKey 체크
        if (paymentRepository.existsByPaymentKey(request.getPaymentKey())) {
            throw new ApplicationException(CommonErrorCode.CONFLICT);
        }
        Payment payment = Payment.createPayment(order, request.getPaymentKey(), request.getAmount());
        payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        order.updateOrderStatus(OrderStatus.COMPLETED);

        return PaymentResponse.from(payment, orderId);
    }
}
