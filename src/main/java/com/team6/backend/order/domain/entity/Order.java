package com.team6.backend.order.domain.entity;


import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /*
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;
     */

    @Column(nullable = false, length = 20)
    private String orderType = "ONLINE";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false)
    private Integer totalPrice = 0;

    private String requestText;

    /*
    public static Order createOrder(User user, Store store, Address address, String requestText) {
        Order order = new Order();
        order.user = user;
        order.store = store;
        order.address = address;
        order.requestText = requestText;
        return order;
    }
     */

    public void updateToTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }
}
