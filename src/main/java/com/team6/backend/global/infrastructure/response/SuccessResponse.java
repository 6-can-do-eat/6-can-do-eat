package com.team6.backend.global.infrastructure.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"code", "message", "data"})
public class SuccessResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private SuccessResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> SuccessResponse<T> of(SuccessCode successCode, T data) {
        return new SuccessResponse<>(successCode.getCode(), successCode.getMessage(), data);
    }

    public static <T> SuccessResponse<T> ok(T data) {
        return of(CommonSuccessCode.OK, data);
    }

    public static <T> SuccessResponse<T> created(T data) {
        return of(CommonSuccessCode.CREATED, data);
    }

    public static <T> SuccessResponse<T> accepted(T data) {
        return of(CommonSuccessCode.ACCEPTED, data);
    }

    public static <T> SuccessResponse<T> noContent() {
        return of(CommonSuccessCode.NO_CONTENT, null);
    }
}