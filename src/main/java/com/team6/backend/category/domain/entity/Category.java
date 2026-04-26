package com.team6.backend.category.domain.entity;

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
@Table(name = "p_category")
@SQLRestriction("deleted_at IS NULL")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, length = 50)
    private String name;

    public Category(String name) {
        this.name = name;
    }

    public void update(String name) {
        this.name = name;
    }

}
