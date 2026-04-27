package com.team6.backend.payment.application;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentRepository;
import com.team6.backend.payment.domain.PaymentStatus;
import com.team6.backend.payment.infrastructure.TossPaymentClient;
import com.team6.backend.payment.infrastructure.dto.TossPaymentConfirmResponse;
import com.team6.backend.payment.infrastructure.dto.TossPaymentRequest;
import com.team6.backend.payment.infrastructure.dto.TossPaymentResponse;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public PaymentResponse confirmPayment(UUID orderId, PaymentConfirmRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        // 결제 금액 일치 여부
        if (!Objects.equals(request.getAmount(), order.getTotalPrice())) {
            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
        // 중복 paymentKey 체크
        if (paymentRepository.existsByPaymentKey(request.getPaymentKey())) {
            throw new ApplicationException(CommonErrorCode.CONFLICT);
        }

        /* Toss 결제 연동용
        TossPaymentConfirmResponse response = tossPaymentClient.confirmPayment(request);
        Payment payment = Payment.createPayment(order, response.getPaymentKey(), response.getTotalAmount());
        */

        Payment payment = Payment.createPayment(order, request.getPaymentKey(), request.getAmount());
        payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        order.updateOrderStatus(OrderStatus.COMPLETED);

        return PaymentResponse.from(payment);
    }

    public Page<PaymentResponse> getPayments(UUID userId, Role role, Pageable pageable) {
        return switch (role) {
            // 유저 본인 결제 내역 조회
            case CUSTOMER -> paymentRepository.findAllByOrder_User_Id(userId, pageable)
                    .map(PaymentResponse::from);
            // 전체 결제 내역 조회
            case MANAGER, MASTER -> paymentRepository.findAll(pageable)
                    .map(PaymentResponse::from);
            default -> throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        };
    }

    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        return PaymentResponse.from(payment);
    }

    public void deletePayment(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        payment.markDeleted(userId.toString());
    }

    public TossPaymentResponse createCheckout() {
        TossPaymentRequest request = new TossPaymentRequest(
                "CARD",
                1L,
                "25d0824f-0870-4f60-8b57-825d92ec6726",
                "음식 주문",
                "http://localhost:8080/api/v1/toss/success",
                "http://localhost:8080/api/v1/toss/fail"
        );
        return tossPaymentClient.createPayment(request);
    }
}
