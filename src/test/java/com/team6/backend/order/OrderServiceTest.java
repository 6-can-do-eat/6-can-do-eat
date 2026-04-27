package com.team6.backend.order;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.exception.MenuErrorCode;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.order.application.OrderService;
import com.team6.backend.order.domain.OrderErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.presentation.dto.*;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private SecurityUtils securityUtils;
    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 생성 성공")
    void createOrder_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID chickenMenuId = UUID.randomUUID();
        UUID colaMenuId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Menu chicken = createMenu(store, chickenMenuId, "후라이드 치킨", 18000);
        Menu cola = createMenu(store, colaMenuId, "콜라", 2000);

        OrderCreateRequest request = createOrderCreateRequest(
                storeId,
                addressId,
                "문 앞에 놓아주세요",
                createOrderItemRequest(chickenMenuId, 1),
                createOrderItemRequest(colaMenuId, 2)
        );

        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(addressRepository.findByAdIdAndUser_Id(addressId, userId)).willReturn(Optional.of(address));
        given(menuRepository.findByMenuIdAndStore_StoreId(chickenMenuId, storeId)).willReturn(Optional.of(chicken));
        given(menuRepository.findByMenuIdAndStore_StoreId(colaMenuId, storeId)).willReturn(Optional.of(cola));

        // when
        OrderResponse response = orderService.createOrder(request, userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStoreId()).isEqualTo(storeId);
        assertThat(response.getAddressId()).isEqualTo(addressId);
        assertThat(response.getRequestText()).isEqualTo("문 앞에 놓아주세요");
        assertThat(response.getTotalPrice()).isEqualTo(22000L);
        assertThat(response.getItems()).hasSize(2);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("주문 생성 실패 - 존재하지 않는 메뉴")
    void createOrder_fail_whenMenuNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);

        OrderCreateRequest request = createOrderCreateRequest(
                storeId,
                addressId,
                "문 앞에 놓아주세요",
                createOrderItemRequest(menuId, 1)
        );

        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(addressRepository.findByAdIdAndUser_Id(addressId, userId)).willReturn(Optional.of(address));
        given(menuRepository.findByMenuIdAndStore_StoreId(menuId, storeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MenuErrorCode.MENU_NOT_FOUND.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 - 주소가 해당 사용자 소유가 아님")
    void createOrder_fail_whenAddressDoesNotBelongToUser() {
        // given
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID menuId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);

        OrderCreateRequest request = createOrderCreateRequest(
                storeId,
                addressId,
                "request",
                createOrderItemRequest(menuId, 1)
        );

        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(addressRepository.findByAdIdAndUser_Id(addressId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, userId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CommonErrorCode.RESOURCE_NOT_FOUND.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 요청사항 수정 성공")
    void updateOrder_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "기존 요청사항", 18000L);

        OrderUpdate.Request request = createUpdateRequest("덜 맵게 해주세요");

        given(orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING))
                .willReturn(Optional.of(order));

        // when
        OrderUpdate.Response response = orderService.updateOrder(orderId, request);

        // then
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getRequestText()).isEqualTo("덜 맵게 해주세요");
        assertThat(order.getRequestText()).isEqualTo("덜 맵게 해주세요");
    }

    @Test
    @DisplayName("주문 상태 변경 성공 - 관리자")
    void updateOrderStatus_success_forManager() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "요청사항", 18000L);

        OrderStatusUpdate.Request request = createStatusUpdateRequest(OrderStatus.APPROVED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        OrderStatusUpdate.Response response = orderService.updateOrderStatus(orderId, userId, Role.MANAGER, request);

        // then
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("주문 상태 변경 성공 - 오너")
    void updateOrderStatus_success_forOwner() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(customerId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "요청사항", 18000L);

        OrderStatusUpdate.Request request = createStatusUpdateRequest(OrderStatus.APPROVED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        OrderStatusUpdate.Response response = orderService.updateOrderStatus(orderId, ownerId, Role.OWNER, request);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    @DisplayName("주문 단건 조회 실패 - 고객이 본인 주문이 아님")
    void getOrder_fail_whenCustomerIsNotOrderOwner() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(customerId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "request", 18000L);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(orderId, anotherUserId, Role.CUSTOMER))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(OrderErrorCode.ORDER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("주문 단건 조회 실패 - 사장이 자기 가게 주문이 아님")
    void getOrder_fail_whenOwnerIsNotStoreOwner() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID anotherOwnerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(customerId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "request", 18000L);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.getOrder(orderId, anotherOwnerId, Role.OWNER))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(OrderErrorCode.ORDER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_success() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "요청사항", 18000L);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        OrderCancel.Response response = orderService.cancelOrder(orderId);

        // then
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 완료된 주문")
    void cancelOrder_fail_whenOrderAlreadyCompleted() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();

        User user = createUser(userId);
        Store store = createStore(storeId, ownerId);
        Address address = createAddress(addressId);
        Order order = createOrder(orderId, user, store, address, "request", 18000L);
        order.updateOrderStatus(OrderStatus.COMPLETED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(OrderErrorCode.ORDER_INVALID_STATUS.getMessage());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    private User createUser(UUID userId) {
        User user = new User("customer1", "password", Role.CUSTOMER, "고객");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Store createStore(UUID storeId, UUID ownerId) {
        User owner = new User("owner1", "password", Role.OWNER, "사장님");
        ReflectionTestUtils.setField(owner, "id", ownerId);

        Category category = new Category("테스트 카테고리");
        ReflectionTestUtils.setField(category, "categoryId", UUID.randomUUID());

        Area area = new Area("테스트 지역", "서울시", "강남구", true);
        ReflectionTestUtils.setField(area, "areaId", UUID.randomUUID());

        Store store = new Store(owner, category, area, "테스트 가게", "서울");
        ReflectionTestUtils.setField(store, "storeId", storeId);
        return store;
    }

    private Address createAddress(UUID addressId) {
        Address address = new Address();
        ReflectionTestUtils.setField(address, "adId", addressId);
        return address;
    }

    private Menu createMenu(Store store, UUID menuId, String name, int price) {
        Menu menu = new Menu(store, name, price, "설명");
        ReflectionTestUtils.setField(menu, "menuId", menuId);
        return menu;
    }

    private Order createOrder(
            UUID orderId,
            User user,
            Store store,
            Address address,
            String requestText,
            Long totalPrice
    ) {
        Order order = Order.createOrder(user, store, address, requestText);
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.now());
        order.updateTotalPrice(totalPrice);
        return order;
    }

    private OrderCreateRequest createOrderCreateRequest(
            UUID storeId,
            UUID addressId,
            String requestText,
            OrderItemCreateRequest... itemRequests
    ) {
        OrderCreateRequest request = BeanUtils.instantiateClass(OrderCreateRequest.class);
        ReflectionTestUtils.setField(request, "storeId", storeId);
        ReflectionTestUtils.setField(request, "addressId", addressId);
        ReflectionTestUtils.setField(request, "requestText", requestText);
        ReflectionTestUtils.setField(request, "itemRequests", List.of(itemRequests));
        return request;
    }

    private OrderItemCreateRequest createOrderItemRequest(UUID menuId, Integer quantity) {
        OrderItemCreateRequest request = BeanUtils.instantiateClass(OrderItemCreateRequest.class);
        ReflectionTestUtils.setField(request, "menuId", menuId);
        ReflectionTestUtils.setField(request, "quantity", quantity);
        return request;
    }

    private OrderUpdate.Request createUpdateRequest(String requestText) {
        OrderUpdate.Request request = BeanUtils.instantiateClass(OrderUpdate.Request.class);
        ReflectionTestUtils.setField(request, "requestText", requestText);
        return request;
    }

    private OrderStatusUpdate.Request createStatusUpdateRequest(OrderStatus orderStatus) {
        OrderStatusUpdate.Request request = BeanUtils.instantiateClass(OrderStatusUpdate.Request.class);
        ReflectionTestUtils.setField(request, "orderStatus", orderStatus);
        return request;
    }
}
