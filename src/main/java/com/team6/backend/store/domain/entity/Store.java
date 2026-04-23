package com.team6.backend.store.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_store")
@SQLRestriction("deleted_at IS NULL")
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_id")
    private UUID StoreId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, name = "average_rating")
    private double rating;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    public Store(UUID ownerId, UUID categoryId, UUID areaId, String name, String address) {
        this.ownerId = ownerId;
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
