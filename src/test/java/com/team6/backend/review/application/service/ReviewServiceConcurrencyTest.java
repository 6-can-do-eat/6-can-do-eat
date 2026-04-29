package com.team6.backend.review.application.service;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.area.domain.repository.AreaRepository;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.domain.repository.CategoryRepository;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.util.AuthValidator;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.review.domain.repository.ReviewRepository;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
class ReviewServiceConcurrencyTest {

    @Autowired private ReviewService reviewService;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AddressRepository addressRepository;

    @MockitoBean private SecurityUtils securityUtils;
    @MockitoBean private AuthValidator authValidator;

    private final List<User> createdUsers = new ArrayList<>();
    private final List<Order> createdOrders = new ArrayList<>();
    private final List<Address> createdAddresses = new ArrayList<>();
    private Store createdStore;
    private Category createdCategory;
    private Area createdArea;

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        orderRepository.deleteAll(createdOrders);
        addressRepository.deleteAll(createdAddresses);
        if (createdStore != null) storeRepository.delete(createdStore);
        if (createdArea != null) areaRepository.delete(createdArea);
        if (createdCategory != null) categoryRepository.delete(createdCategory);
        userRepository.deleteAll(createdUsers);
    }

    @Test
    @DisplayName("실제 리뷰의 평균 평점과 DB상에 저장된 평균 평점이 일치해야 한다.")
    void raceConditionTest_WithLock() throws InterruptedException {
        // given
        String randomStr = UUID.randomUUID().toString().substring(0, 8);

        User owner = new User("owner_" + randomStr, "pass", Role.OWNER, "점주님");
        createdUsers.add(userRepository.save(owner));

        createdCategory = categoryRepository.save(new Category("테스트 카테고리 " + randomStr));
        createdArea = areaRepository.save(new Area("테스트 지역 " + randomStr, "서울시", "강남구", true));
        createdStore = storeRepository.save(new Store(owner, createdCategory, createdArea, "동시성 테스트 가게 " + randomStr, "강남구 123"));

        int threadCount = 100;

        for (int i = 0; i < threadCount; i++) {
            User customer = new User("cust_" + randomStr + "_" + i, "pass", Role.CUSTOMER, "고객" + i);
            createdUsers.add(userRepository.save(customer));

            Address address = new Address();
            ReflectionTestUtils.setField(address, "user", customer);
            ReflectionTestUtils.setField(address, "address", "배송지 " + i);
            createdAddresses.add(addressRepository.save(address));

            Order order = Order.createOrder(UUID.randomUUID(), customer, createdStore, address, "조심히 와주세요");
            ReflectionTestUtils.setField(order, "status", OrderStatus.COMPLETED);
            createdOrders.add(orderRepository.save(order));
        }

        doNothing().when(authValidator).validateAccess(any(), any(), any(), any());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            final UUID orderId = createdOrders.get(i).getId();

            executorService.submit(() -> {
                try {
                    startLatch.await();

                    int randomRating = ThreadLocalRandom.current().nextInt(1, 6);

                    ReviewRequestDto request = new ReviewRequestDto();
                    ReflectionTestUtils.setField(request, "rating", randomRating);
                    ReflectionTestUtils.setField(request, "content", randomRating + "점 드립니다!");

                    reviewService.createReview(orderId, request);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();

        // then
        Store updatedStore = storeRepository.findById(createdStore.getStoreId()).orElseThrow();

        Double realAverage = reviewRepository.calculateAverageRatingByStoreId(createdStore.getStoreId());
        double roundedRealAverage = Math.round(realAverage * 10) / 10.0;

        System.out.println("100개 리뷰의 진짜 평균: " + roundedRealAverage);
        System.out.println("DB에 저장된 Store 평점: " + updatedStore.getRating());

        assertThat(updatedStore.getRating()).isEqualTo(roundedRealAverage);
    }
}
