package com.team6.backend.store.domain.entity;

import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.user.domain.entity.User;
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
    private UUID storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, name = "average_rating")
    private double rating;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    public Store(User owner, Category category, Area area, String name, String address) {
        this.owner = owner;
        this.category = category;
        this.area = area;
        this.name = name;
        this.address = address;
        this.rating = 0.0;
        this.isHidden = false;
    }

    public void update(Category category, Area area, String name, String address) {
        this.category = category;
        this.area = area;
        this.name = name;
        this.address = address;
    }

    public void hideStore() {
        this.isHidden = !this.isHidden;
    }

    public void updateRating(double averageRating) {
        this.rating = Math.round(averageRating * 10) / 10.0;
    }
}
