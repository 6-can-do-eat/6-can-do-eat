package com.team6.backend.review.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 리뷰 작성·수정 요청 바디(생성/수정 동일 구조) */
@Getter
@Setter
@NoArgsConstructor
public class ReviewRequestDto {

    @Schema(description = "평점 (1~5점 사이)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "평점은 반드시 입력해야 합니다.")
    @Min(1) @Max(5)
    private Integer rating; // 1~5

    @Schema(description = "리뷰 본문 (최대 500자)", example = "음식이 정말 맛있고 배달이 빨라요!", nullable = true)
    @Size(max = 500, message = "리뷰는 500자 이내로 작성해야 합니다.")
    private String content; // null 허용(본문 생략)


}


