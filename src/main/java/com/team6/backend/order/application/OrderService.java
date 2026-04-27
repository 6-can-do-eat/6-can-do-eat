package com.team6.backend.order.application;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.exception.MenuErrorCode;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.order.domain.OrderErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.entity.OrderItem;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.presentation.dto.*;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.exception.StoreErrorCode;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, UUID userId) {
        log.info("주문 생성 요청: userId={}", userId);
        // 멱등성 키로 주문 조회
        Order existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);

        if (existingOrder != null) {
            List<OrderItem> existingItems = orderItemRepository.findByOrderId(existingOrder.getId());
            return OrderResponse.from(existingOrder, existingOrder.getUser().getId(), existingItems);
        }

        // 인증된 사용자는 getReferenceById로 참조
        User user = userRepository.getReferenceById(userId);

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> {
                    log.warn("주문 생성 실패/가게 없음: storeId={}", request.getStoreId());
                    return new ApplicationException(StoreErrorCode.STORE_NOT_FOUND);
                });
        // 가게 영업 여부 확인
        validateStoreOrderable(store);
        // 해당 주소가 인증된 사용자의 주소와 동일한지 확인
        Address address = addressRepository.findByAdIdAndUser_Id(request.getAddressId(), userId)
                .orElseThrow(() -> {
                    log.warn("주문 생성 실패/주소 불일치: addressId={}", request.getAddressId());
                    return new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
                });;

        Order order = Order.createOrder(request.getIdempotencyKey(), user, store, address, request.getRequestText());

        List<OrderItem> orderItems = request.getItemRequests().stream().map(
                itemRequest -> {
                    // 모든 메뉴가 같은 가게 인지 여부 확인
                    Menu menu = menuRepository.findByMenuIdAndStore_StoreId(itemRequest.getMenuId(), store.getStoreId())
                            .orElseThrow(() -> {
                                log.warn("주문 생성 실패/가게 메뉴 불일치: menuId={}, storeId={}", itemRequest.getMenuId(), store.getStoreId());
                                return new ApplicationException(MenuErrorCode.MENU_NOT_FOUND);
                            });
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

        log.info("주문 생성 완료: orderId={}", order.getId());

        return OrderResponse.from(order, userId, orderItems);
    }

    public Page<OrderResponse> getOrders(UUID userId, Role role, Pageable pageable) {
        log.info("주문 목록 조회 요청");

        Page<Order> orders = switch (role) {
            case CUSTOMER -> orderRepository.findAllByUserId(userId, pageable);
            case OWNER -> orderRepository.findAllByStore_OwnerId(userId, pageable);
            case MANAGER, MASTER -> orderRepository.findAll(pageable);
            default -> {
                log.warn("주문 목록 조회 실패/권한 없음: userId={}, role={}", userId, role);
                throw new ApplicationException(OrderErrorCode.ORDER_FORBIDDEN);
            }
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

    public OrderResponse getOrder(UUID orderId, UUID userId, Role role) {
        log.info("주문 단건 조회 요청: orderId={}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> {
                    log.warn("주문 단건 조회 실패/주문 없음: orderId={}", orderId);
                    return new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND);
                }
        );
        // 사용자의 주문이 맞는지 확인
        if (role == Role.CUSTOMER && !userId.equals(order.getUser().getId())) {
            log.warn("주문 단건 조회 실패/사용자 권한 위반: orderId={}, userId={}", orderId, userId);
            throw new ApplicationException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        // 가게에 포함되는 주문 일치 여부 확인
        if (role == Role.OWNER && !userId.equals(order.getStore().getOwner().getId())) {
            log.warn("주문 단건 조회 실패/오너 권한 위반: orderId={}, userId={}", orderId, userId);
            throw new ApplicationException(OrderErrorCode.ORDER_FORBIDDEN);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, order.getUser().getId(), orderItems);
    }

    @Transactional
    public OrderUpdate.Response updateOrder(UUID orderId, @Valid OrderUpdate.Request request) {
        log.info("주문 요청사항 수정 요청: orderId={}", orderId);
        // Order 상태 PENDING 여부 확인
        Order order = orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING).orElseThrow(
                () -> {
                    log.warn("주문 요청사항 수정 실패/주문 없음: orderId={}", orderId);
                    return new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND);
                }
        );
        order.updateRequestText(request.getRequestText());

        log.info("주문 요청사항 수정 완료: orderId={}", orderId);
        return OrderUpdate.Response.from(orderId, request.getRequestText());
    }

    @Transactional
    public OrderStatusUpdate.Response updateOrderStatus(UUID orderId, UUID userId, Role role, OrderStatusUpdate.Request request) {
        log.info("주문 상태 변경 요청: orderId={}", orderId);

        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(
                () -> {
                    log.warn("주문 상태 변경 실패/주문 없음: orderId={}", orderId);
                    return new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND);
                }
        );

        if (role == Role.OWNER && !userId.equals(order.getStore().getOwner().getId())) {
            log.warn("주문 상태 변경 실패/오너 권한 위반: orderId={}, userId={}", orderId, userId);
            throw new ApplicationException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        order.updateOrderStatus(request.getOrderStatus());

        log.info("주문 상태 변경 완료: orderId={}, status={}", orderId, order.getStatus());
        return OrderStatusUpdate.Response.from(order.getId(), order.getStatus());
    }

    @Transactional
    public OrderCancel.Response cancelOrder(UUID orderId) {
        log.info("주문 취소 요청: orderId={}", orderId);

        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(
                () -> {
                    log.warn("주문 취소 실패/주문 없음: orderId={}", orderId);
                    return new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND);
                }
        );
        // 주문 취소 5분 이내 제한
        order.validateCancelable();
        order.updateOrderStatus(OrderStatus.CANCELLED);

        log.info("주문 취소 완료: orderId={}", orderId);
        return OrderCancel.Response.from(orderId, order.getStatus());
    }

    @Transactional
    public void deleteOrder(UUID orderId, UUID userId) {
        log.info("주문 삭제 요청: orderId={}", orderId);

        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new ApplicationException(OrderErrorCode.ORDER_NOT_FOUND)
        );
        order.markDeleted(userId.toString());
        log.info("주문 삭제 완료: orderId={}, deletedBy={}", orderId, userId);
    }

    private void validateStoreOrderable(Store store) {
        if (store.isHidden() || store.isDeleted())
            throw new ApplicationException(OrderErrorCode.STORE_NOT_ORDERABLE);
    }

    private void validateMenuOrderable(Menu menu) {
        if (menu.isHidden() || menu.isDeleted())
            throw new ApplicationException(OrderErrorCode.MENU_NOT_ORDERABLE);
    }
}
