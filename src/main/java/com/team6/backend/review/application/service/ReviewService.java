package com.team6.backend.review.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.review.domain.entity.ReviewEntity;
import com.team6.backend.review.domain.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 주문(1:1) 기준으로 작성·조회·수정·소프트 삭제하는 리뷰 유스케이스.
 * 작성자 검증(본인 주문), 역할별 삭제 권한은 본 서비스에서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;


    @Transactional
    public ReviewResponseDto createReview(UUID orderId, ReviewRequestDto reviewRequestDto) {


        UUID userId = securityUtils.getCurrentUserId();

        // 경로에 넘어온 주문이 DB에 있어야 함
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        // 리뷰는 “내 주문”에만 달 수 있음(다른 사람 주문 ID로 생성 시도 차단)
        if (!userId.equals(order.getUser().getId())) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        // 동일 주문으로 이미 리뷰가 있으면 중복 방지
        if (reviewRepository.existsByOrder_Id(orderId)) {
            throw new ApplicationException(CommonErrorCode.CONFLICT);
        }

//        // 픽업 완료 상태에만 리뷰 작성 가능
//        if (order.getStatus() != OrderStatus.COMPLETED) {
//            throw new ApplicationException(CommonErrorCode.INVALID_INPUT_VALUE);
//        }

        ReviewEntity review = new ReviewEntity();
        review.createReview(
                order,
                reviewRequestDto.getRating(),
                reviewRequestDto.getContent()
        );
        ReviewEntity saved = reviewRepository.save(review);

        return new ReviewResponseDto(saved);

    }

    // 삭제(소프트)된 리뷰는 존재하지 않는 것과 동일하게 처리
    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(UUID reviewId) {

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
        if (review.isDeleted()) {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }
        return new ReviewResponseDto(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviews(UUID storeId, int page, int size, String sortBy, boolean isAsc) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                isAsc ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending()
        );

        Page<ReviewEntity> reviewList = reviewRepository.findByStore_IdAndDeletedAtIsNull(storeId, pageable);

        return reviewList.map(ReviewResponseDto::new);
    }

    // 리뷰 작성자(고객)만 수정 가능
    @Transactional
    public ReviewResponseDto updateReview(UUID reviewId, ReviewRequestDto request) {

        UUID userId = securityUtils.getCurrentUserId();

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (review.isDeleted()) {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND);

        }

        if (!review.getUser().getId().equals(userId)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
        review.update(request.getRating(), request.getContent());

        return new ReviewResponseDto(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {

        UUID userId = securityUtils.getCurrentUserId();

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        Role role = securityUtils.getCurrentUserRole();

        // @PreAuthorize로 막혀도 서비스에서 역할을 한 번 더 제한(방어)
        if (role != Role.CUSTOMER && role != Role.MANAGER && role != Role.MASTER) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        // 고객은 본인이 작성한 리뷰만
        if (role == Role.CUSTOMER && !review.getUser().getId().equals(userId)) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }

        review.delete(String.valueOf(userId));
    }
}