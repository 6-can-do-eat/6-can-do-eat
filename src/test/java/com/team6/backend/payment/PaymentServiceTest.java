package com.team6.backend.payment;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.application.PaymentService;
import com.team6.backend.payment.domain.Payment;
import com.team6.backend.payment.domain.PaymentErrorCode;
import com.team6.backend.payment.domain.PaymentRepository;
import com.team6.backend.payment.domain.PaymentStatus;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 승인 성공")
    void confirmPayment_success() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 15_000L, OrderStatus.PENDING);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 15_000L);
        Payment payment = createPayment(UUID.randomUUID(), order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.insertPaymentIfAbsent(
                any(UUID.class),
                eq(orderId),
                eq("payment-key-1"),
                eq(15_000L),
                eq(userId.toString())
        )).willReturn(1);
        given(paymentRepository.findById(any(UUID.class))).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.confirmPayment(orderId, request);

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-1");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(15_000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        verify(paymentRepository).insertPaymentIfAbsent(
                any(UUID.class),
                eq(orderId),
                eq("payment-key-1"),
                eq(15_000L),
                eq(userId.toString())
        );
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제 금액 불일치")
    void confirmPayment_fail_whenAmountDoesNotMatch() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 15_000L, OrderStatus.PENDING);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 12_000L);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(orderId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).insertPaymentIfAbsent(
                any(UUID.class),
                any(UUID.class),
                any(String.class),
                any(Long.class),
                any(String.class)
        );
    }

    @Test
    @DisplayName("결제 승인 성공 - 완료된 주문에 같은 결제 키로 재시도하면 기존 결제를 반환한다")
    void confirmPayment_returnExistingPayment_whenCompletedOrderIsRetriedWithSamePaymentKey() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 15_000L, OrderStatus.COMPLETED);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 15_000L);
        Payment payment = createPayment(UUID.randomUUID(), order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.confirmPayment(orderId, request);

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        verify(paymentRepository, never()).insertPaymentIfAbsent(
                any(UUID.class),
                any(UUID.class),
                any(String.class),
                any(Long.class),
                any(String.class)
        );
    }

    @Test
    @DisplayName("결제 승인 실패 - 완료된 주문에 다른 결제 키로 요청하면 충돌한다")
    void confirmPayment_fail_whenCompletedOrderUsesDifferentPaymentKey() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 15_000L, OrderStatus.COMPLETED);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-2", 15_000L);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.findByPaymentKey("payment-key-2")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(orderId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_KEY_ALREADY_EXISTS.getMessage());

        verify(paymentRepository, never()).insertPaymentIfAbsent(
                any(UUID.class),
                any(UUID.class),
                any(String.class),
                any(Long.class),
                any(String.class)
        );
    }

    @Test
    @DisplayName("결제 승인 성공 - upsert 경합에서 진 요청은 기존 결제를 반환한다")
    void confirmPayment_returnExistingPayment_whenInsertReturnsZero() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(orderId, userId, 15_000L, OrderStatus.PENDING);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 15_000L);
        Payment payment = createPayment(UUID.randomUUID(), order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(paymentRepository.insertPaymentIfAbsent(
                any(UUID.class),
                eq(orderId),
                eq("payment-key-1"),
                eq(15_000L),
                eq(userId.toString())
        )).willReturn(0);
        given(paymentRepository.findByPaymentKey("payment-key-1")).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.confirmPayment(orderId, request);

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("결제 단건 조회 성공")
    void getPayment_success() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, UUID.randomUUID(), 15_000L, OrderStatus.COMPLETED);
        Payment payment = createPayment(paymentId, order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(paymentId, UUID.randomUUID(), Role.MANAGER);

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-1");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("결제 단건 조회 실패 - 고객이 본인 결제가 아님")
    void getPayment_fail_whenCustomerIsNotPaymentOwner() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Order order = createOrder(orderId, customerId, 15_000L, OrderStatus.COMPLETED);
        Payment payment = createPayment(paymentId, order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPayment(paymentId, anotherUserId, Role.CUSTOMER))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("결제 상태 변경 실패 - 완료 후 취소 불가")
    void updatePaymentStatus_fail_whenCompletedToCancelled() {
        Order order = createOrder(UUID.randomUUID(), UUID.randomUUID(), 15_000L, OrderStatus.COMPLETED);
        Payment payment = createPayment(
                UUID.randomUUID(),
                order,
                "payment-key-1",
                15_000L,
                PaymentStatus.COMPLETED
        );

        assertThatThrownBy(() -> payment.updatePaymentStatus(PaymentStatus.CANCELLED))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_INVALID_STATUS.getMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제 삭제 성공")
    void deletePayment_success() {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = createOrder(UUID.randomUUID(), UUID.randomUUID(), 15_000L, OrderStatus.COMPLETED);
        Payment payment = createPayment(paymentId, order, "payment-key-1", 15_000L, PaymentStatus.COMPLETED);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        paymentService.deletePayment(paymentId, userId);

        assertThat(payment.isDeleted()).isTrue();
        assertThat(payment.getDeletedBy()).isEqualTo(userId.toString());
    }

    private PaymentConfirmRequest createConfirmRequest(UUID orderId, String paymentKey, Long amount) {
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(request, "orderId", orderId.toString());
        ReflectionTestUtils.setField(request, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(request, "amount", amount);
        return request;
    }

    private Order createOrder(UUID orderId, UUID userId, Long totalPrice, OrderStatus status) {
        User user = new User("customer1", "password", Role.CUSTOMER, "고객");
        ReflectionTestUtils.setField(user, "id", userId);

        Store store = mock(Store.class);
        Address address = mock(Address.class);

        Order order = Order.createOrder(UUID.randomUUID(), user, store, address, "요청사항");
        ReflectionTestUtils.setField(order, "id", orderId);
        order.updateTotalPrice(totalPrice);
        if (status != OrderStatus.PENDING) {
            order.updateOrderStatus(status);
        }
        return order;
    }

    private Payment createPayment(
            UUID paymentId,
            Order order,
            String paymentKey,
            Long amount,
            PaymentStatus paymentStatus
    ) {
        Payment payment = Payment.createPayment(order, paymentKey, amount);
        if (paymentStatus != PaymentStatus.PENDING) {
            payment.updatePaymentStatus(paymentStatus);
        }
        ReflectionTestUtils.setField(payment, "id", paymentId);
        return payment;
    }
}
