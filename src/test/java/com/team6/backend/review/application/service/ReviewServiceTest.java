package com.team6.backend.review.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.util.AuthValidator;
import com.team6.backend.order.domain.OrderStatus;
import com.team6.backend.order.domain.entity.Order;
import com.team6.backend.order.domain.repository.OrderRepository;
import com.team6.backend.review.domain.entity.Review;
import com.team6.backend.review.domain.exception.ReviewErrorCode;
import com.team6.backend.review.domain.repository.ReviewRepository;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuthValidator authValidator;

    private UUID userId;
    private UUID orderId;
    private UUID reviewId;
    private UUID storeId;
    private User mockUser;
    private Store mockStore;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        mockUser = mock(User.class);
        mockStore = mock(Store.class);
        mockOrder = mock(Order.class);
    }

    private ReviewRequestDto createReviewRequestDto(int rating, String content) {
        ReviewRequestDto request = new ReviewRequestDto();
        ReflectionTestUtils.setField(request, "rating", rating);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    @Test
    @DisplayName("리뷰 작성 성공 - 완료된 주문에 대해 본인이 리뷰를 작성할 수 있다.")
    void createReview_Success() {
        // given
        ReviewRequestDto request = createReviewRequestDto(5, "맛있어요!");

        given(mockUser.getId()).willReturn(userId);
        given(mockStore.getStoreId()).willReturn(storeId);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getUser()).willReturn(mockUser);
        given(mockOrder.getStatus()).willReturn(OrderStatus.COMPLETED);
        given(mockOrder.getStore()).willReturn(mockStore);
        given(reviewRepository.existsByOrder_Id(orderId)).willReturn(false);

        Review savedReview = new Review();
        ReflectionTestUtils.setField(savedReview, "id", reviewId);
        ReflectionTestUtils.setField(savedReview, "user", mockUser);
        ReflectionTestUtils.setField(savedReview, "store", mockStore);
        ReflectionTestUtils.setField(savedReview, "order", mockOrder);
        ReflectionTestUtils.setField(savedReview, "rating", 5);
        ReflectionTestUtils.setField(savedReview, "content", "맛있어요!");

        given(reviewRepository.save(any(Review.class))).willReturn(savedReview);
        given(reviewRepository.calculateAverageRatingByStoreId(storeId)).willReturn(5.0);

        // when
        ReviewResponseDto response = reviewService.createReview(orderId, request);

        // then
        assertThat(response).isNotNull();
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewRepository, times(1)).flush();
        verify(mockStore, times(1)).updateRating(5.0);
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 본인의 주문이 아닌 경우 예외가 발생한다.")
    void createReview_Fail_Forbidden() {
        // given
        UUID anotherUserId = UUID.randomUUID();
        ReviewRequestDto request = createReviewRequestDto(5, "맛있어요!");

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
        given(mockOrder.getUser()).willReturn(mockUser);
        given(mockUser.getId()).willReturn(userId); // 실제 주문자의 ID

        doThrow(new ApplicationException(ReviewErrorCode.REVIEW_FORBIDDEN))
                .when(authValidator).validateAccess(any(), any(), any(), any());

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> reviewService.createReview(orderId, request));
        assertThat(exception.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 주문 상태가 COMPLETED가 아닌 경우 예외가 발생한다.")
    void createReview_Fail_NotCompleted() {
        // given
        ReviewRequestDto request = createReviewRequestDto(5, "맛있어요!");

        // given(securityUtils.getCurrentUserId()).willReturn(userId);
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
        // given(mockOrder.getUser()).willReturn(mockUser);
        lenient().when(mockOrder.getUser()).thenReturn(mockUser);
        given(mockUser.getId()).willReturn(userId);
        given(reviewRepository.existsByOrder_Id(orderId)).willReturn(false);
        given(mockOrder.getStatus()).willReturn(OrderStatus.PENDING); // 완료 상태 아님

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> reviewService.createReview(orderId, request));
        assertThat(exception.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_BAD_REQUEST);
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 매니저는 본인의 리뷰가 아니어도 삭제할 수 있다.")
    void deleteReview_Manager_Success() {
        // given
        Review review = mock(Review.class);

        given(mockStore.getStoreId()).willReturn(storeId);

        given(securityUtils.getCurrentUserId()).willReturn(UUID.randomUUID()); // 작성자가 아님
        // given(securityUtils.getCurrentUserRole()).willReturn(Role.MANAGER);
        lenient().when(securityUtils.getCurrentUserRole()).thenReturn(Role.MANAGER);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(review.getStore()).willReturn(mockStore);
        given(review.getUser()).willReturn(mockUser);

        // when
        reviewService.deleteReview(reviewId);

        // then
        verify(review).delete(anyString());
        verify(reviewRepository, times(1)).flush();
        verify(reviewRepository, times(1)).calculateAverageRatingByStoreId(storeId);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 고객은 타인이 작성한 리뷰를 삭제할 수 없다.")
    void deleteReview_Customer_Fail_Forbidden() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID authorUserId = UUID.randomUUID();

        Review review = mock(Review.class);
        User author = mock(User.class);

        given(securityUtils.getCurrentUserId()).willReturn(currentUserId);
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        given(review.getUser()).willReturn(author);
        given(author.getId()).willReturn(authorUserId); // 작성자 ID와 현재 사용자 ID가 다름

        doThrow(new ApplicationException(ReviewErrorCode.REVIEW_FORBIDDEN))
                .when(authValidator).validateAccess(any(), any(), any(), any());

        // when & then
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> reviewService.deleteReview(reviewId));
        assertThat(exception.getErrorCode()).isEqualTo(ReviewErrorCode.REVIEW_FORBIDDEN);
    }
}