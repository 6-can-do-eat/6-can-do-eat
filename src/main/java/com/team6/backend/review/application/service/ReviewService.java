package com.team6.backend.review.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.util.AuthValidator;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.review.domain.exception.ReviewErrorCode;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.review.domain.entity.ReviewEntity;
import com.team6.backend.review.domain.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 주문(1:1) 기준으로 작성·조회·수정·소프트 삭제하는 리뷰 유스케이스.
 * 작성자 검증(본인 주문), 역할별 삭제 권한은 본 서비스에서 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtils securityUtils;
    private final AuthValidator authValidator;

    @Transactional
    public ReviewResponseDto createReview(UUID orderId, ReviewRequestDto reviewRequestDto) {

        // 경로에 넘어온 주문이 DB에 있어야 함
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("[REVIEW] 리뷰 생성 실패: 주문을 찾을 수 없습니다. orderId: {}", orderId);
                    return new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
                });

        // 리뷰는 “내 주문”에만 달 수 있음(다른 사람 주문 ID로 생성 시도 차단)
        authValidator.validateAccess(
                order.getUser().getId(),
                null,
                List.of(Role.CUSTOMER),
                ReviewErrorCode.REVIEW_FORBIDDEN
        );

        // 동일 주문으로 이미 리뷰가 있으면 중복 방지
        if (reviewRepository.existsByOrder_Id(orderId)) {
            log.warn("[REVIEW] 리뷰 생성 실패: 이미 리뷰가 존재하는 주문입니다. orderId: {}", orderId);
            throw new ApplicationException(ReviewErrorCode.REVIEW_CONFLICT);
        }

        // 픽업 완료 상태에만 리뷰 작성 가능
        if (order.getStatus() != OrderStatus.COMPLETED) {
            log.warn("[REVIEW] 리뷰 생성 실패: 완료되지 않은 주문입니다. orderId: {}, status: {}", orderId, order.getStatus());
            throw new ApplicationException(ReviewErrorCode.REVIEW_BAD_REQUEST);
        }

        ReviewEntity review = new ReviewEntity();
        review.createReview(
                order,
                reviewRequestDto.getRating(),
                reviewRequestDto.getContent()
        );
        ReviewEntity saved = reviewRepository.save(review);

        // 리뷰 생성 후 영속성 컨텍스트를 DB에 플러시하여 집계 쿼리에 포함되도록 한 후 평균 평점 갱신
        reviewRepository.flush();
        updateStoreAverageRating(order.getStore());

        return new ReviewResponseDto(saved);

    }

    // 삭제(소프트)된 리뷰는 존재하지 않는 것과 동일하게 처리
    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(UUID reviewId) {

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("[REVIEW] 리뷰 조회 실패: 리뷰를 찾을 수 없습니다. reviewId: {}", reviewId);
                    return new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
                });

        if (review.isDeleted()) {
            log.warn("[REVIEW] 리뷰 조회 실패: 삭제된 리뷰입니다. reviewId: {}", reviewId);
            throw new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
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

        Page<ReviewEntity> reviewList = reviewRepository.findByStore_StoreIdAndDeletedAtIsNull(storeId, pageable);

        return reviewList.map(ReviewResponseDto::new);
    }

    // 리뷰 작성자(고객)만 수정 가능
    @Transactional
    public ReviewResponseDto updateReview(UUID reviewId, ReviewRequestDto request) {

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("[REVIEW] 리뷰 수정 실패: 리뷰를 찾을 수 없습니다. reviewId: {}", reviewId);
                    return new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
                });

        if (review.isDeleted()) {
            log.warn("[REVIEW] 리뷰 수정 실패: 삭제된 리뷰입니다. reviewId: {}", reviewId);
            throw new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
        }

        authValidator.validateAccess(
                review.getUser().getId(),
                null,
                List.of(Role.CUSTOMER),
                ReviewErrorCode.REVIEW_FORBIDDEN
        );

        review.update(request.getRating(), request.getContent());

        reviewRepository.flush();
        updateStoreAverageRating(review.getStore());

        return new ReviewResponseDto(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {

        UUID userId = securityUtils.getCurrentUserId();

        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("[REVIEW] 리뷰 삭제 실패: 리뷰를 찾을 수 없습니다. reviewId: {}", reviewId);
                    return new ApplicationException(ReviewErrorCode.REVIEW_NOT_FOUND);
                });

        Role role = securityUtils.getCurrentUserRole();

        // @PreAuthorize로 막혀도 서비스에서 역할을 한 번 더 제한(방어)
        authValidator.validateAccess(
                review.getUser().getId(),
                List.of(Role.MANAGER, Role.MASTER),
                List.of(Role.CUSTOMER), // 고객은 본인이 작성한 리뷰만
                ReviewErrorCode.REVIEW_FORBIDDEN
        );

        review.delete(String.valueOf(userId));

        reviewRepository.flush();
        updateStoreAverageRating(review.getStore());
    }

    private void updateStoreAverageRating(Store store) {
        Double averageRating = reviewRepository.calculateAverageRatingByStoreId(store.getStoreId());
        store.updateRating(averageRating);
    }
}