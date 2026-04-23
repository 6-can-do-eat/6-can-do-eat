package com.team6.backend.order.application;

import com.team6.backend.address.domain.entity.Address;
import com.team6.backend.address.domain.repository.AddressRepository;
import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.order.domain.entity.OrderItem;
import com.team6.backend.order.domain.repository.OrderItemRepository;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.presentation.dto.OrderCreateRequest;
import com.team6.backend.order.presentation.dto.OrderResponse;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final AddressRepository addressRepository;

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





    private void validateStoreOrderable(Store store) {
        if (store.isHidden() || store.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    private void validateMenuOrderable(Menu menu) {
        if (menu.isHidden() || menu.isDeleted())
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

}
