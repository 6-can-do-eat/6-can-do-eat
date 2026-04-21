package com.team6.backend.order.application;

import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.order.domain.entity.OrderItem;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.presentation.dto.OrderCreateRequest;
import com.team6.backend.order.presentation.dto.OrderResponse;
import com.team6.backend.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    /*
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, UUID userId) {
        User user = userRepository.getReferenceById(userId);

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
        validateStoreOrderable(store);

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));;

        Order order = Order.createOrder(user, store, address, request.getRequestText());

        List<OrderItem> orderItems = request.getItemRequests().stream().map(
                itemRequest -> {
                    Menu menu = menuRepository.findById(itemRequest.getMenuId())
                            .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
                    validateMenuOrderable(menu);
                    return OrderItem.createOrderItem(order, menu, itemRequest.getQuantity(), menu.getPrice());
                }
        ).toList();

        int totalPrice = orderItems.stream()
                .mapToInt(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
        order.updateToTotalPrice(totalPrice);

        orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        return OrderResponse.from(order, userId, orderItems);
    }

    private void validateStoreOrderable(Store store) {
        if (store.isHidden() || store.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    private void validateMenuOrderable(Menu menu) {
        if (menu.isHidden() || menu.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
    */
}
