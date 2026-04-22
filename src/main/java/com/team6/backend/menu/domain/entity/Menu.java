package com.team6.backend.menu.domain.entity;

import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "menu_id")
    private UUID menuId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    public Menu(UUID storeId, String name, int price, String description) {
        this.storeId = storeId;
        this.name = name;
        this.price = price;
        this.description = description;
        this.isHidden = false;
    }

    public void update(UpdateMenuRequest request) {
        this.name = request.getName();
        this.price = request.getPrice();
        this.description = request.getDescription();
    }

    public void hideMenu() {
        this.isHidden = !this.isHidden;
    }
}
