package com.team6.backend.review.domain.exception;

import com.team6.backend.global.infrastructure.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_BAD_REQUEST("REVIEW_400", HttpStatus.BAD_REQUEST, "배달 완료된 주문에 대해서만 리뷰를 작성할 수 있습니다."),
    REVIEW_FORBIDDEN("REVIEW_403", HttpStatus.FORBIDDEN, "리뷰에 대한 권한이 없습니다."),
    REVIEW_NOT_FOUND("REVIEW_404", HttpStatus.NOT_FOUND, "해당 리뷰가 존재하지 않습니다."),
    REVIEW_CONFLICT("REVIEW_409", HttpStatus.CONFLICT, "해당 주문에 대한 리뷰가 이미 존재합니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

}
