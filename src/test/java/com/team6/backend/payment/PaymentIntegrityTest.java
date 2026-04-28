package com.team6.backend.payment;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.area.domain.repository.AreaRepository;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.domain.repository.CategoryRepository;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtUtil;
import com.team6.backend.global.infrastructure.redis.RedisService;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.payment.application.PaymentService;
import com.team6.backend.payment.domain.PaymentRepository;
import com.team6.backend.payment.infrastructure.TossPaymentClient;
import com.team6.backend.payment.presetation.dto.PaymentConfirmRequest;
import com.team6.backend.payment.presetation.dto.PaymentResponse;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentIntegrityTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private AddressRepository addressRepository;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @Test
    @DisplayName("멱등성 요구사항 - 같은 결제 승인 요청 재시도는 기존 결제를 반환해야 한다")
    void confirmPayment_shouldReturnExistingPayment_whenSameRequestIsRetried() {
        Order order = savePendingOrder(15_000L);
        PaymentConfirmRequest request = paymentRequest(order.getId(), "payment-key-" + UUID.randomUUID(), 15_000L);

        PaymentResponse first = paymentService.confirmPayment(order.getId(), request);
        PaymentResponse second = paymentService.confirmPayment(order.getId(), request);

        assertThat(second.getPaymentId()).isEqualTo(first.getPaymentId());
        assertThat(paymentCountByOrderId(order.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("동시멱등성 요구사항 - 같은 결제 승인 요청이 동시에 들어와도 모두 같은 결제를 반환해야 한다")
    void confirmPayment_shouldReturnSamePayment_whenSameRequestArrivesConcurrently() throws Exception {
        Order order = savePendingOrder(15_000L);
        String paymentKey = "payment-key-" + UUID.randomUUID();
        List<PaymentResponse> responses = new CopyOnWriteArrayList<>();
        List<String> outcomes = new CopyOnWriteArrayList<>();

        List<Throwable> failures = runConcurrently(10, index -> {
            PaymentConfirmRequest request = paymentRequest(order.getId(), paymentKey, 15_000L);

            try {
                PaymentResponse response = paymentService.confirmPayment(order.getId(), request);
                responses.add(response);
                outcomes.add("success: index=" + index
                        + ", paymentKey=" + paymentKey
                        + ", paymentId=" + response.getPaymentId());
            } catch (Exception e) {
                outcomes.add("fail: index=" + index
                        + ", paymentKey=" + paymentKey
                        + ", exception=" + e.getClass().getSimpleName()
                        + ", message=" + e.getMessage());
                throw e;
            }
        });

        String outcomeSummary = summarizeOutcomes(outcomes);
        System.out.println("same payment key race outcomes: " + outcomeSummary);

        assertThat(failures)
                .as("race outcomes: %s", outcomeSummary)
                .isEmpty();
        assertThat(responses)
                .as("race outcomes: %s", outcomeSummary)
                .hasSize(10);
        assertThat(responses)
                .as("race outcomes: %s", outcomeSummary)
                .extracting(PaymentResponse::getPaymentId)
                .containsOnly(responses.get(0).getPaymentId());
        assertThat(paymentCountByOrderId(order.getId()))
                .as("race outcomes: %s", outcomeSummary)
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("동시성 요구사항 - 같은 주문에 서로 다른 결제 키가 동시에 들어와도 결제는 하나만 성공해야 한다")
    void confirmPayment_shouldAllowOnlyOnePayment_whenDifferentPaymentKeysRaceForSameOrder() throws Exception {
        Order order = savePendingOrder(15_000L);
        List<String> outcomes = new CopyOnWriteArrayList<>();

        List<Throwable> failures = runConcurrently(10, index -> {
            String paymentKey = "payment-key-" + index + "-" + UUID.randomUUID();
            PaymentConfirmRequest request = paymentRequest(
                    order.getId(),
                    paymentKey,
                    15_000L
            );

            try {
                PaymentResponse response = paymentService.confirmPayment(order.getId(), request);
                outcomes.add("success: index=" + index
                        + ", paymentKey=" + paymentKey
                        + ", paymentId=" + response.getPaymentId());
            } catch (Exception e) {
                outcomes.add("fail: index=" + index
                        + ", paymentKey=" + paymentKey
                        + ", exception=" + e.getClass().getSimpleName()
                        + ", message=" + e.getMessage());
                throw e;
            }
        });

        long successCount = 10L - failures.size();
        Order reloadedOrder = orderRepository.findById(order.getId()).orElseThrow();
        String outcomeSummary = summarizeOutcomes(outcomes);
        System.out.println("different payment key race outcomes: " + outcomeSummary);

        assertThat(successCount)
                .as("race outcomes: %s", outcomeSummary)
                .isEqualTo(1L);
        assertThat(paymentCountByOrderId(order.getId()))
                .as("race outcomes: %s", outcomeSummary)
                .isEqualTo(1L);
        assertThat(reloadedOrder.getStatus())
                .as("race outcomes: %s", outcomeSummary)
                .isEqualTo(OrderStatus.COMPLETED);
    }

    private Order savePendingOrder(Long totalPrice) {
        User customer = userRepository.save(new User(
                "customer-" + UUID.randomUUID(),
                "password",
                Role.CUSTOMER,
                "customer-" + UUID.randomUUID()
        ));
        User owner = userRepository.save(new User(
                "owner-" + UUID.randomUUID(),
                "password",
                Role.OWNER,
                "owner-" + UUID.randomUUID()
        ));
        Category category = categoryRepository.save(new Category("category-" + UUID.randomUUID()));
        Area area = areaRepository.save(new Area("area-" + UUID.randomUUID(), "서울", "강남구", true));
        Store store = storeRepository.save(new Store(owner, category, area, "store-" + UUID.randomUUID(), "서울시"));
        Address address = addressRepository.save(new Address(
                new AddressRequest("서울시 강남구", "101호", false, "집"),
                customer
        ));

        Order order = Order.createOrder(UUID.randomUUID(), customer, store, address, "요청사항");
        order.updateTotalPrice(totalPrice);
        return orderRepository.saveAndFlush(order);
    }

    private PaymentConfirmRequest paymentRequest(UUID orderId, String paymentKey, Long amount) {
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(request, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(request, "orderId", orderId.toString());
        ReflectionTestUtils.setField(request, "amount", amount);
        return request;
    }

    private long paymentCountByOrderId(UUID orderId) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getOrder().getId().equals(orderId))
                .count();
    }

    private String summarizeOutcomes(List<String> outcomes) {
        long successCount = outcomes.stream().filter(outcome -> outcome.startsWith("success:")).count();
        long failCount = outcomes.stream().filter(outcome -> outcome.startsWith("fail:")).count();
        return "successCount=" + successCount
                + ", failCount=" + failCount
                + ", details=" + outcomes;
    }

    private List<Throwable> runConcurrently(int threadCount, ThrowingConsumer<Integer> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("동시 실행 시작 대기 시간이 초과되었습니다.");
                }
                task.accept(index);
                return null;
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Throwable> failures = new ArrayList<>();
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                failures.add(e.getCause() == null ? e : e.getCause());
            }
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        return failures;
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }
}
