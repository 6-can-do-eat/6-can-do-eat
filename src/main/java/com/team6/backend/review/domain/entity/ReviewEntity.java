package com.team6.backend.review.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class ReviewEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID id;

    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Store store;

    @JoinColumn(name = "order_id", nullable = false, updatable = false, unique = true)
    @OneToOne(fetch = FetchType.LAZY)
    private Order order;

    @Column(name = "rating", nullable = false) // null 값 불가
    private Integer rating;

    @Column(name = "content", length = 500) // 리뷰 글자수 500자 제한
    private String content;

    public void createReview(Order order, int rating, String content) {
        validRating(rating);
        this.user = order.getUser();
        this.order = order;
        this.store = order.getStore();
        this.rating = rating;
        this.content = content;
    }

    private void validRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("별점 1점에서 5점 사이만 가능합니다.");
        }
    }

    public void update(Integer rating, String content) {
        validRating(rating);
        this.rating = rating;
        this.content = content;
    }

    public void delete(String deletedBy) {
        markDeleted(deletedBy);
    }
}
