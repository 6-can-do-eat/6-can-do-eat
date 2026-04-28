//package com.team6.backend.review.application.service;
//
//import com.team6.backend.address.domain.entity.Address;
//import com.team6.backend.address.domain.repository.AddressRepository;
//import com.team6.backend.area.domain.entity.Area;
//import com.team6.backend.area.domain.repository.AreaRepository;
//import com.team6.backend.auth.domain.repository.UserRepository;
//import com.team6.backend.category.domain.entity.Category;
//import com.team6.backend.category.domain.repository.CategoryRepository;
//import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
//import com.team6.backend.global.infrastructure.util.AuthValidator;
//import com.team6.backend.order.domain.OrderStatus;
//import com.team6.backend.order.domain.entity.Order;
//import com.team6.backend.order.domain.repository.OrderRepository;
//import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
//import com.team6.backend.store.domain.entity.Store;
//import com.team6.backend.store.domain.repository.StoreRepository;
//import com.team6.backend.user.domain.entity.Role;
//import com.team6.backend.user.domain.entity.User;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.BDDMockito.given;
//
//@SpringBootTest
//@Transactional
//class ReviewServiceIntegrationTest {
//
//    @Autowired
//    private ReviewService reviewService;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private StoreRepository storeRepository;
//
//    @Autowired
//    private CategoryRepository categoryRepository;
//
//    @Autowired
//    private AreaRepository areaRepository;
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private AddressRepository addressRepository;
//
//    @MockitoBean
//    private SecurityUtils securityUtils;
//
//    @MockitoBean
//    private AuthValidator authValidator;
//
//    private User createMockUser(String username, String password, Role role) {
//        User user = new User();
//        ReflectionTestUtils.setField(user, "username", username);
//        ReflectionTestUtils.setField(user, "password", password);
//        ReflectionTestUtils.setField(user, "role", role);
//        return user;
//    }
//
//    private Order createMockOrder(User user, Store store, Address address) {
//        Order order = Order.createOrder(user, store, address, null);
//        ReflectionTestUtils.setField(order, "orderType", "ONLINE");
//        ReflectionTestUtils.setField(order, "status", OrderStatus.COMPLETED);
//        return order;
//    }
//
//    private ReviewRequestDto createMockReviewRequestDto(int rating, String content) {
//        ReviewRequestDto requestDto = new ReviewRequestDto();
//        ReflectionTestUtils.setField(requestDto, "rating", rating);
//        ReflectionTestUtils.setField(requestDto, "content", content);
//        return requestDto;
//    }
//
//    @Test
//    @DisplayName("리뷰 작성 서비스 로직 실행 시, DB의 가게 평균 평점이 실제로 갱신되어야 한다.")
//    void createReview_updatesStoreRating_realIntegrationTest() {
//        User user = createMockUser("testUser1", "testPassword1", Role.CUSTOMER);
//        userRepository.save(user);
//
//        User owner = createMockUser("testUser2", "testPassword2", Role.OWNER);
//        userRepository.save(owner);
//
//        Category category = new Category("테스트 카테고리");
//        categoryRepository.save(category);
//
//        Area area = new Area("테스트 지역", "_", "_", true);
//        areaRepository.save(area);
//
//        Store store = new Store(owner, category, area, "평점 테스트 가게", "테스트 주소");
//        storeRepository.save(store);
//
//        Address address = new Address();
//        ReflectionTestUtils.setField(address, "user", user);
//        ReflectionTestUtils.setField(address, "address", "테스트 배송지");
//        addressRepository.save(address);
//
//        // 리뷰 1 생성
//        Order order1 = createMockOrder(user, store, address);
//        orderRepository.save(order1);
//
//        given(securityUtils.getCurrentUserId()).willReturn(user.getId());
//
//        ReviewRequestDto request1 = createMockReviewRequestDto(5, "정말 맛있어요");
//        reviewService.createReview(order1.getId(), request1);
//
//        storeRepository.flush();
//        Store updatedStore = storeRepository.findById(store.getStoreId()).orElseThrow();
//        assertThat(updatedStore.getRating()).isEqualTo(5.0);
//
//        // 리뷰 2 생성
//        Order order2 = createMockOrder(user, store, address);
//        orderRepository.save(order2);
//        ReviewRequestDto request2 = createMockReviewRequestDto(4, "맛있네요");
//        reviewService.createReview(order2.getId(), request2);
//
//        storeRepository.flush();
//        updatedStore = storeRepository.findById(store.getStoreId()).orElseThrow();
//        assertThat(updatedStore.getRating()).isEqualTo(4.5);
//
//        // 리뷰 3 생성
//        Order order3 = createMockOrder(user, store, address);
//        orderRepository.save(order3);
//        ReviewRequestDto request3 = createMockReviewRequestDto(1, "머리카락 나옴");
//        reviewService.createReview(order3.getId(), request3);
//
//        storeRepository.flush();
//        updatedStore = storeRepository.findById(store.getStoreId()).orElseThrow();
//        assertThat(updatedStore.getRating()).isEqualTo(3.3);
//    }
//}