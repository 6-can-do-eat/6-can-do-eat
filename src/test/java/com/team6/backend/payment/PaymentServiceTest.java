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
        // given
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, 15000L);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 15000L);

        given(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).willReturn(Optional.of(order));
        given(paymentRepository.existsByPaymentKey("payment-key-1")).willReturn(false);

        // when
        PaymentResponse response = paymentService.confirmPayment(orderId, request);

        // then
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-1");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(15000L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제 금액 불일치")
    void confirmPayment_fail_whenAmountDoesNotMatch() {
        // given
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, 15000L);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 12000L);

        given(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(orderId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 승인 실패 - 이미 사용된 결제 키")
    void confirmPayment_fail_whenPaymentKeyAlreadyExists() {
        // given
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(orderId, 15000L);
        PaymentConfirmRequest request = createConfirmRequest(orderId, "payment-key-1", 15000L);

        given(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)).willReturn(Optional.of(order));
        given(paymentRepository.existsByPaymentKey("payment-key-1")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(orderId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_KEY_ALREADY_EXISTS.getMessage());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 단건 조회 성공")
    void getPayment_success() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = createPayment(paymentId, orderId, "payment-key-1", 15000L, PaymentStatus.COMPLETED);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        // when
        PaymentResponse response = paymentService.getPayment(paymentId, UUID.randomUUID(), Role.MANAGER);

        // then
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getPaymentKey()).isEqualTo("payment-key-1");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("결제 단건 조회 실패 - 고객이 본인 결제가 아님")
    void getPayment_fail_whenCustomerIsNotPaymentOwner() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Payment payment = createPayment(
                paymentId,
                orderId,
                "payment-key-1",
                15000L,
                PaymentStatus.COMPLETED,
                customerId
        );

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.getPayment(paymentId, anotherUserId, Role.CUSTOMER))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("결제 상태 변경 실패 - 완료 후 취소 불가")
    void updatePaymentStatus_fail_whenCompletedToCancelled() {
        // given
        Payment payment = createPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "payment-key-1",
                15000L,
                PaymentStatus.COMPLETED
        );

        // when & then
        assertThatThrownBy(() -> payment.updatePaymentStatus(PaymentStatus.CANCELLED))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(PaymentErrorCode.PAYMENT_INVALID_STATUS.getMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("결제 삭제 성공")
    void deletePayment_success() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payment payment = createPayment(
                paymentId,
                UUID.randomUUID(),
                "payment-key-1",
                15000L,
                PaymentStatus.COMPLETED
        );

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        // when
        paymentService.deletePayment(paymentId, userId);

        // then
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

    private Order createOrder(UUID orderId, Long totalPrice) {
        User user = mock(User.class);
        Store store = mock(Store.class);
        Address address = mock(Address.class);

        Order order = Order.createOrder(user, store, address, "요청사항");
        ReflectionTestUtils.setField(order, "id", orderId);
        order.updateTotalPrice(totalPrice);
        return order;
    }

    private Payment createPayment(
            UUID paymentId,
            UUID orderId,
            String paymentKey,
            Long amount,
            PaymentStatus paymentStatus
    ) {
        Order order = createOrder(orderId, amount);
        Payment payment = Payment.createPayment(order, paymentKey, amount);
        payment.updatePaymentStatus(paymentStatus);
        ReflectionTestUtils.setField(payment, "id", paymentId);
        return payment;
    }

    private Payment createPayment(
            UUID paymentId,
            UUID orderId,
            String paymentKey,
            Long amount,
            PaymentStatus paymentStatus,
            UUID userId
    ) {
        Order order = createOrder(orderId, amount, userId);
        Payment payment = Payment.createPayment(order, paymentKey, amount);
        payment.updatePaymentStatus(paymentStatus);
        ReflectionTestUtils.setField(payment, "id", paymentId);
        return payment;
    }

    private Order createOrder(UUID orderId, Long totalPrice, UUID userId) {
        User user = new User("customer1", "password", Role.CUSTOMER, "customer");
        ReflectionTestUtils.setField(user, "id", userId);
        Store store = mock(Store.class);
        Address address = mock(Address.class);

        Order order = Order.createOrder(user, store, address, "?붿껌?ы빆");
        ReflectionTestUtils.setField(order, "id", orderId);
        order.updateTotalPrice(totalPrice);
        return order;
    }
}
