package com.team6.backend.store.domain.entity;

import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.auth.domain.entity.User;
import com.team6.backend.category.domain.entity.Category;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private UUID id;

    // TODO: User에 1:N 연관관계 추가
     @ManyToOne(fetch = FetchType.LAZY, optional = false)
     @JoinColumn(name = "owner_id", nullable = false)
     private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @OneToMany(mappedBy = "store")
    private List<Menu> menus;

    // TODO: Order에 N:1 연관관계 추가
    // @OneToMany(mappedBy = "store")
    // private List<Order> orders;

    // TODO: Review에 N:1 연관관계 추가
    // @OneToMany(mappedBy = "store")
    // private List<Review> reviews;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, name = "average_rating")
    private double rating;

    @Column(nullable = false)
    private boolean is_hidden;

    public Store(User owner, Category category, Area area, String name, String address, double rating, boolean is_hidden) {
        this.owner = owner;
        this.category = category;
        this.area = area;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.is_hidden = is_hidden;
    }

}
