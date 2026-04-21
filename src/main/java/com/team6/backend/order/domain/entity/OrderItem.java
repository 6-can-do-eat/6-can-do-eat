package com.team6.backend.order.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.*;
import java.util.UUID;

@Entity
@Table(name = "p_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer unitPrice;

    public static OrderItem createOrderItem(Order order, Menu menu, Integer quantity, Integer unitPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.order = order;
        orderItem.menu = menu;
        orderItem.quantity = quantity;
        orderItem.unitPrice = unitPrice;
        return orderItem;
    }
}
