package com.team6.backend.menu.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.store.domain.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_menu")
@SQLRestriction("deleted_at IS NULL")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "menu_id")
    private UUID menuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String description;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    public Menu(Store store, String name, int price, String description) {
        this.store = store;
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
