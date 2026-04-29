package com.team6.backend.ai.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductDescriptionRequest {

    @Schema(description = "AI 프롬프트", example = " 치킨 메뉴의 이름을 추천해줘", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "프롬프트는 필수입니다.")
    @Size(max = 100, message = "프롬프트는 최대 100자까지 입력할 수 있습니다.")
    private String prompt;
}