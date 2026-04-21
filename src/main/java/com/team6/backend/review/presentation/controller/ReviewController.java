package com.team6.backend.review.presentation.controller;

import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.review.application.service.ReviewService;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/orders/{orderId}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> createReview(@PathVariable UUID orderId, @RequestBody @Valid ReviewRequestDto requestDto){

        ReviewResponseDto response = reviewService.createReview(orderId, requestDto);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + response.getReviewId())).body(SuccessResponse.created(response));
    }

    // 리뷰 단일 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> getReview(@PathVariable UUID reviewId){

        ReviewResponseDto response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }


    // 가게별 리뷰 목록 조회
    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<SuccessResponse<Page<ReviewResponseDto>>> getReviews(@PathVariable UUID storeId,
                                                                               @RequestParam("page") int page,
                                                                               @RequestParam("size") int size,
                                                                               @RequestParam("sortBy") String sortBy,
                                                                               @RequestParam("isAsc") boolean isAsc
    ){
        return ResponseEntity.ok(SuccessResponse.ok(reviewService.getReviews(storeId, page, size, sortBy, isAsc)));
    }

    //가게 리뷰 수정
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> updateReview(@PathVariable UUID reviewId, @RequestBody @Valid ReviewRequestDto reviewRequestDto){
        return ResponseEntity.ok(SuccessResponse.ok(reviewService.updateReview(reviewId, reviewRequestDto)));
    }

    //리뷰 삭제(204, 바디 없음)
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER','MASTER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

}
