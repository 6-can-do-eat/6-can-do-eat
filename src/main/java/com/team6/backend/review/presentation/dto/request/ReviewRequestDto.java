package com.team6.backend.review.presentation.dto.request;

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

    @NotNull(message = "평점은 반드시 입력해야 합니다.")
    @Min(1) @Max(5)
    private Integer rating; // 1~5

    @Size(max = 500, message = "리뷰는 500자 이내로 작성해야 합니다.")
    private String content; // null 허용(본문 생략)


}


