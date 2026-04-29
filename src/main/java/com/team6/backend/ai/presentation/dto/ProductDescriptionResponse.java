package com.team6.backend.ai.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ProductDescriptionResponse {

    @Schema(description = "요청 프롬프트", example = "치킨 메뉴의 이름을 추천해줘")
    private final String prompt;
    @Schema(description = "AI 응답 결과", example = "후라이드 치킨, 양념 치킨")
    private final String result;

    public ProductDescriptionResponse(String prompt, String result) {
        this.prompt = prompt;
        this.result = result;
    }
}
