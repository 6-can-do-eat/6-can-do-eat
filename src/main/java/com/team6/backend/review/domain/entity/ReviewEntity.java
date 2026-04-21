package com.team6.backend.review.domain.entity;

import com.team6.backend.global.infrastructure.entity.BaseEntity;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

public class ReviewEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="review_id", updatable =false)
    private UUID id;

    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "store_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Store store;

//    @JoinColumn(name = "order_id", nullable = false, updatable = false)
//    @ManyToOne(fetch = FetchType.LAZY)
//    private Order order;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", length = 500)
    private String content;

//    @Builder
//    public static ReviewEntity create(User user, Order order, Store store, int rating, String content) {
//
//        ReviewEntity review = new ReviewEntity();
//        review.user = Objects.requireNonNull(user, "user must not be null");
//        review.order = Objects.requireNonNull(order, "order must not be null");
//        review.store = Objects.requireNonNull(store, "store must not be null");
//        validRating(rating);
//        review.rating = rating;
//        review.content = content;
//
//        return review;
//    }

    private static void validRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
    }

    public void update(int rating, String content){
        this.rating = rating;
        this.content = content;
    }

    public void delete(String deletedBy){
        markDeleted(deletedBy);
    }

}
