package com.team6.backend.payment.application;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.order.domain.OrderErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentErrorCode;
import com.team6.backend.payment.domain.PaymentRepository;
import com.team6.backend.payment.domain.PaymentStatus;
import com.team6.backend.payment.infrastructure.TossPaymentClient;
import com.team6.backend.payment.infrastructure.dto.TossPaymentRequest;
import com.team6.backend.payment.infrastructure.dto.TossPaymentResponse;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public PaymentResponse confirmPayment(UUID orderId, PaymentConfirmRequest request) {
        log.info("결제 승인 요청: orderId={}, paymentKey={}", orderId, request.getPaymentKey());

        Order order = orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING).orElseThrow(
                () -> {
                    log.warn("결제 승인 실패/주문 없음: orderId={}", orderId);
                    return new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND);
                }
        );
        // 결제 금액 일치 여부
        if (!Objects.equals(request.getAmount(), order.getTotalPrice())) {
            log.warn("결제 승인 실패/금액 불일치: orderId={}, requestAmount={}, orderAmount={}",
                    orderId, request.getAmount(), order.getTotalPrice());
            throw new ApplicationException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        // 중복 paymentKey 체크
        if (paymentRepository.existsByPaymentKey(request.getPaymentKey())) {
            log.warn("결제 승인 실패 - 중복 결제 키: orderId={}, paymentKey={}",
                    orderId, request.getPaymentKey());
            throw new ApplicationException(PaymentErrorCode.PAYMENT_KEY_ALREADY_EXISTS);
        }

        /* Toss 결제 연동용
        TossPaymentConfirmResponse response = tossPaymentClient.confirmPayment(request);
        Payment payment = Payment.createPayment(order, response.getPaymentKey(), response.getTotalAmount());
        */

        Payment payment = Payment.createPayment(order, request.getPaymentKey(), request.getAmount());
        payment.updatePaymentStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        order.updateOrderStatus(OrderStatus.COMPLETED);

        log.info("결제 승인 완료: paymentId={}, orderId={}", payment.getId(), orderId);

        return PaymentResponse.from(payment);
    }

    public Page<PaymentResponse> getPayments(UUID userId, Role role, Pageable pageable) {
        log.info("결제 목록 조회 요청");
        return switch (role) {
            // 유저 본인 결제 내역 조회
            case CUSTOMER -> paymentRepository.findAllByOrder_User_Id(userId, pageable)
                    .map(PaymentResponse::from);
            // 전체 결제 내역 조회
            case MANAGER, MASTER -> paymentRepository.findAll(pageable)
                    .map(PaymentResponse::from);
            default -> {
                log.warn("결제 목록 조회 실패/권한 없음: userId={}, role={}", userId, role);
                throw new ApplicationException(PaymentErrorCode.PAYMENT_FORBIDDEN);
            }
        };
    }

    public PaymentResponse getPayment(UUID paymentId, UUID userId, Role role) {
        log.info("결제 단건 조회 요청: paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> {
                    log.warn("결제 단건 조회 실패/결제 없음: paymentId={}", paymentId);
                    return new ApplicationException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                }
        );
        if (role == Role.CUSTOMER && !userId.equals(payment.getOrder().getUser().getId())) {
            log.warn("결제 단건 조회 실패/사용자 권한 위반: paymentId={}, userId={}", paymentId, userId);
            throw new ApplicationException(PaymentErrorCode.PAYMENT_FORBIDDEN);
        }
        return PaymentResponse.from(payment);
    }

    public void deletePayment(UUID paymentId, UUID userId) {
        log.info("결제 삭제 요청: paymentId={}, userId={}", paymentId, userId);

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> {
                    log.warn("결제 삭제 실패/결제 없음: paymentId={}", paymentId);
                    return new ApplicationException(PaymentErrorCode.PAYMENT_NOT_FOUND);
                }
        );
        payment.markDeleted(userId.toString());
        log.info("결제 삭제 완료: paymentId={}, deletedBy={}", paymentId, userId);
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
