package com.team6.backend.review.presentation.dto.response;

import com.team6.backend.review.domain.entity.ReviewEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@NoArgsConstructor
public class ReviewResponseDto {
    private UUID reviewId;
    private UUID userId;
    private UUID storeId;
    private UUID orderId;
    private String content;
    private Integer rating;

    private List<ReviewResponseDto> reviewList = new ArrayList<>();

    public ReviewResponseDto(ReviewEntity review) {
        this.reviewId = review.getId();
        this.userId = review.getUser().getId();
        this.storeId = review.getStore().getStoreId();
        this.orderId = review.getOrder().getId();
        this.content = review.getContent();
        this.rating = review.getRating();
    }
}


