package com.team6.backend.review.presentation.dto.response;

import com.team6.backend.review.domain.entity.ReviewEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ReviewResponseDto {

    @Schema(description = "리뷰 고유 ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID reviewId;

    @Schema(description = "작성자(고객) ID", example = "ce3dde08-47da-4d44-b619-abd1f73f45ba")
    private UUID userId;

    @Schema(description = "가게 ID", example = "238ec5c7-bc05-48ac-924a-34dc306acc04")
    private UUID storeId;

    @Schema(description = "주문 ID", example = "040b4329-7674-4e65-b767-a8e44adeeeae")
    private UUID orderId;

    @Schema(description = "리뷰 내용", example = "맛있게 잘 먹었습니다.")
    private String content;

    @Schema(description = "평점", example = "5")
    private Integer rating;

    @Schema(description = "연관 리뷰 리스트 (현재는 단일 조회 시 빈 리스트로 반환될 수 있음)")
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
