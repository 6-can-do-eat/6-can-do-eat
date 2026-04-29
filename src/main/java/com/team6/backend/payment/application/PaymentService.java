package com.team6.backend.payment.application;

import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.order.domain.OrderErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentErrorCode;
import com.team6.backend.payment.domain.PaymentRepository;
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

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND));

        // 결제 금액과 주문 금액 일치 여부
        if (!Objects.equals(request.getAmount(), order.getTotalPrice())) {
            throw new ApplicationException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 이미 PENDING이 아닌 주문은 "새 결제 진행" 대상 X
        // 여기서 같은 paymentKey로 재시도한 경우라면 기존 Payment를 반환하고,
        // 같은 orderId에 다른 paymentKey로 들어온 경우라면 조회가 안 되므로 충돌 처리
        if (order.getStatus() != OrderStatus.PENDING) {
            return paymentRepository.findByPaymentKey(request.getPaymentKey())
                    .map(PaymentResponse::from)
                    .orElseThrow(() -> new ApplicationException(PaymentErrorCode.PAYMENT_KEY_ALREADY_EXISTS));
        }

        /* Toss 결제 연동용
        TossPaymentConfirmResponse response = tossPaymentClient.confirmPayment(request);
        Payment payment = Payment.createPayment(order, response.getPaymentKey(), response.getTotalAmount());
        */

        UUID paymentId = UUID.randomUUID();

        // 결제 생성은 DB upsert 쿼리
        // 같은 paymentKey 동시 요청이면 payment_key unique 충돌 시 do nothing
        // 같은 orderId에 다른 paymentKey 동시 요청이면 order_id unique 충돌 시 do nothing
        // 반환값이 1이면 내가 실제 insert에 성공한 요청이고, 0이면 이미 다른 요청이 선점한 상태
        int inserted = paymentRepository.insertPaymentIfAbsent(
                paymentId,
                orderId,
                request.getPaymentKey(),
                request.getAmount(),
                order.getUser().getId().toString()
        );

        // insert에 성공한 경우에만 주문 상태를 COMPLETED로 변경
        if (inserted == 1) {
            order.updateOrderStatus(OrderStatus.COMPLETED);

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ApplicationException(PaymentErrorCode.PAYMENT_NOT_FOUND));
            log.info("결제 승인 완료: paymentId={}, orderId={}", payment.getId(), orderId);
            return PaymentResponse.from(payment);
        }

        // insert에 실패한 경우:
        // 같은 paymentKey 요청이 먼저 성공했다면 기존 Payment를 반환
        // 같은 orderId에 다른 paymentKey 요청이 먼저 성공했다면 조회 결과가 없으므로 충돌로 처리
        return paymentRepository.findByPaymentKey(request.getPaymentKey())
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ApplicationException(PaymentErrorCode.PAYMENT_KEY_ALREADY_EXISTS));
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
