package com.team6.backend.order.application;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.entity.OrderItem;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.presentation.dto.*;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final AddressRepository addressRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, UUID userId) {
        // 인증된 사용자는 getReferenceById로 참조
        User user = userRepository.getReferenceById(userId);

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
        // 가게 영업 여부 확인
        validateStoreOrderable(store);

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));;

        Order order = Order.createOrder(user, store, address, request.getRequestText());

        List<OrderItem> orderItems = request.getItemRequests().stream().map(
                itemRequest -> {
                    Menu menu = menuRepository.findById(itemRequest.getMenuId())
                            .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
                    // 메뉴 활성화 여부 확인
                    validateMenuOrderable(menu);
                    return OrderItem.createOrderItem(order, menu, itemRequest.getQuantity(), menu.getPrice());
                }
        ).toList();

        Long totalPrice = orderItems.stream()
                .mapToLong(item -> (long) item.getQuantity() * item.getUnitPrice())
                .sum();
        order.updateTotalPrice(totalPrice);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        return OrderResponse.from(order, userId, orderItems);
    }

    public Page<OrderResponse> getOrders(UUID userId, Pageable pageable) {
        Role role = securityUtils.getCurrentUserRole();
        Page<Order> orders = switch (role) {
            case CUSTOMER -> orderRepository.findAllByUserId(userId, pageable);
            case OWNER -> orderRepository.findAllByStore_OwnerId(userId, pageable);
            case MANAGER, MASTER -> orderRepository.findAll(pageable);
            default -> throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        };
        // Order 하나에 해당하는 List<OrderItem>를 한번에 불러오기 위한 로직
        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        // N+1 방지를 위해 조회하려는 Order ID를 리스트로 검색해서 해당하는 List<OrderItem>을 한번에 다 가져와서 그룹핑
        Map<UUID, List<OrderItem>> orderItemsMap = orderItemRepository.findAllByOrder_IdIn(orderIds)
                .stream().collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.map(order -> OrderResponse.from(
                order,
                userId,
                orderItemsMap.getOrDefault(order.getId(), List.of())
        ));
    }

    public OrderResponse getOrder(UUID orderId) {
        Role role = securityUtils.getCurrentUserRole();
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        // 가게에 포함되는 주문 일치 여부 확인
        if (role == Role.OWNER && !securityUtils.getCurrentUserId().equals(order.getUser().getId())) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, order.getUser().getId(), orderItems);
    }

    @Transactional
    public OrderUpdate.Response updateOrder(UUID orderId, @Valid OrderUpdate.Request request) {
        // Order 상태 PENDING 여부 확인
        Order order = orderRepository.findByIdAndOrderStatus(orderId, OrderStatus.PENDING).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        order.updateRequestText(request.getRequestText());
        return OrderUpdate.Response.from(orderId, request.getRequestText());
    }

    @Transactional
    public OrderStatusUpdate.Response updateOrderStatus(UUID orderId, OrderStatusUpdate.Request request) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );

        Role role = securityUtils.getCurrentUserRole();
        if (role == Role.OWNER && !securityUtils.getCurrentUserId().equals(order.getStore().getOwnerId())) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
        order.updateOrderStatus(request.getOrderStatus());

        return OrderStatusUpdate.Response.from(order.getId(), order.getOrderStatus());
    }

    @Transactional
    public OrderCancel.Response cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        order.updateOrderStatus(OrderStatus.CANCELLED);
        return OrderCancel.Response.from(orderId, order.getOrderStatus());
    }

    @Transactional
    public void deleteOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND)
        );
        order.markDeleted(securityUtils.getCurrentUserId().toString());
    }

    private void validateStoreOrderable(Store store) {
        if (store.isHidden() || store.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    private void validateMenuOrderable(Menu menu) {
        if (menu.isHidden() || menu.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
}
