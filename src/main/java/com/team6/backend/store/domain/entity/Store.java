package com.team6.backend.store.domain.entity;

import com.team6.backend.user.domain.entity.User;
import com.team6.backend.menu.domain.entity.Menu;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @OneToMany(mappedBy = "store")
    private List<Menu> menus;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, name = "average_rating")
    private double rating;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    public Store(User owner, UUID categoryId, UUID areaId, String name, String address) {
        this.owner = owner;
        this.categoryId = categoryId;
        this.areaId = areaId;
        this.name = name;
        this.address = address;
        this.rating = 0.0;
        this.isHidden = false;
    }

    public void update(UUID categoryId, UUID areaId, String name, String address) {
        this.categoryId = categoryId;
        this.areaId = areaId;
        this.name = name;
        this.address = address;
    }

    public void hideStore() {
        this.isHidden = !this.isHidden;
    }
}
