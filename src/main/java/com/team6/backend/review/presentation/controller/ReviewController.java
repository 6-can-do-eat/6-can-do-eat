package com.team6.backend.review.presentation.controller;

import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.review.application.service.ReviewService;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Review", description = "리뷰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "리뷰 작성",
            description = "특정 주문에 대한 리뷰를 작성합니다. 작성 성공 시 가게의 평균 평점이 갱신됩니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `CUSTOMER` (본인의 주문에 대해서만 가능)\n" +
                    "- **상태 제약**: 주문 상태가 `COMPLETED`(픽업 완료)인 경우에만 작성할 수 있습니다.\n" +
                    "- **중복 방지**: 한 주문당 하나의 리뷰만 작성할 수 있습니다.\n\n" +
                    "**[발생 가능한 비즈니스 에러]**\n" +
                    "- `REVIEW_NOT_FOUND`: 주문 ID가 존재하지 않을 때\n" +
                    "- `REVIEW_FORBIDDEN`: 타인의 주문에 리뷰를 남기려 할 때\n" +
                    "- `REVIEW_CONFLICT`: 해당 주문에 이미 리뷰가 등록되어 있을 때\n" +
                    "- `REVIEW_BAD_REQUEST`: 주문이 완료 상태가 아닐 때",
            responses = {
                    @ApiResponse(responseCode = "201", description = "리뷰 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "입력값 오류 또는 주문 미완료 상태",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_BAD_REQUEST\", \"status\": \"BAD_REQUEST\", \"message\": \"완료된 주문에 대해서만 리뷰를 작성할 수 있습니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "권한 부족 (작성자 불일치)",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_FORBIDDEN\", \"status\": \"FORBIDDEN\", \"message\": \"본인의 주문에 대해서만 리뷰를 남길 수 있습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "주문 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_NOT_FOUND\", \"status\": \"NOT_FOUND\", \"message\": \"대상을 찾을 수 없습니다.\"}"))),
                    @ApiResponse(responseCode = "409", description = "리뷰 중복",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_CONFLICT\", \"status\": \"CONFLICT\", \"message\": \"이미 해당 주문에 대한 리뷰가 존재합니다.\"}")))
            }
    )
    @PostMapping("/orders/{orderId}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> createReview(
            @Parameter(description = "주문 고유 ID") @PathVariable UUID orderId,
            @RequestBody @Valid ReviewRequestDto requestDto){

        ReviewResponseDto response = reviewService.createReview(orderId, requestDto);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + response.getReviewId())).body(SuccessResponse.created(response));
    }

    @Operation(
            summary = "리뷰 단일 조회",
            description = "리뷰 ID를 통해 상세 내용을 조회합니다. 소프트 삭제된 리뷰는 조회되지 않습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "리뷰 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_NOT_FOUND\", \"status\": \"NOT_FOUND\", \"message\": \"존재하지 않거나 삭제된 리뷰입니다.\"}")))
            }
    )
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> getReview(
            @Parameter(description = "리뷰 고유 ID") @PathVariable UUID reviewId){

        ReviewResponseDto response = reviewService.getReview(reviewId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    @Operation(
            summary = "가게별 리뷰 목록 조회",
            description = "특정 가게에 등록된 리뷰 목록을 페이징 조회합니다. 삭제된 리뷰는 결과에서 제외됩니다.",
            parameters = {
                    @Parameter(name = "storeId", description = "가게 ID"),
                    @Parameter(name = "page", description = "페이지 번호 (0부터 시작)"),
                    @Parameter(name = "size", description = "페이지 당 데이터 개수"),
                    @Parameter(name = "sortBy", description = "정렬 필드 (createdAt, rating 등)"),
                    @Parameter(name = "isAsc", description = "오름차순 여부 (false 시 최신순/높은순)")
            },
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "404", description = "가게 없음",
                        content = @Content(examples = @ExampleObject(value = "{\"code\": \"STORE_NOT_FOUND\", \"status\": \"NOT_FOUND\", \"message\": \"존재하지 않는 가게입니다.\"}")))
            }
    )
    @GetMapping("/stores/{storeId}/reviews")
    public ResponseEntity<SuccessResponse<Page<ReviewResponseDto>>> getReviews(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean isAsc
    ){
        return ResponseEntity.ok(SuccessResponse.ok(reviewService.getReviews(storeId, page, size, sortBy, isAsc)));
    }

    @Operation(
            summary = "리뷰 수정",
            description = "작성된 리뷰의 평점과 내용을 수정합니다. 수정 완료 시 가게의 평균 평점이 재계산됩니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `CUSTOMER` (본인 작성 리뷰만 가능)\n" +
                    "- 삭제된 리뷰는 수정할 수 없습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "입력값 오류",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_400\", \"status\": \"BAD_REQUEST\", \"message\": \"평점은 반드시 입력해야 합니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_403\", \"status\": \"FORBIDDEN\", \"message\": \"리뷰 수정 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "리뷰 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_404\", \"status\": \"NOT_FOUND\", \"message\": \"수정할 리뷰를 찾을 수 없습니다.\"}")))
            }
    )
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponse<ReviewResponseDto>> updateReview(
            @Parameter(description = "수정할 리뷰 ID") @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewRequestDto reviewRequestDto){
        return ResponseEntity.ok(SuccessResponse.ok(reviewService.updateReview(reviewId, reviewRequestDto)));
    }

    @Operation(
            summary = "리뷰 삭제 (소프트 삭제)",
            description = "리뷰를 시스템에서 삭제 처리(Soft Delete)합니다. 삭제 시 가게의 평균 평점이 즉시 갱신됩니다.\n\n" +
                    "**[권한 정책]**\n" +
                    "- `CUSTOMER`: 본인이 작성한 리뷰만 삭제 가능\n" +
                    "- `MANAGER`, `MASTER`: 모든 리뷰 삭제 가능",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_FORBIDDEN\", \"status\": \"FORBIDDEN\", \"message\": \"리뷰 삭제 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "리뷰 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"REVIEW_NOT_FOUND\", \"status\": \"NOT_FOUND\", \"message\": \"이미 삭제되었거나 존재하지 않는 리뷰입니다.\"}")))
            }
    )
    @PreAuthorize("hasAnyRole('CUSTOMER','MANAGER','MASTER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "삭제할 리뷰 ID") @PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

}
