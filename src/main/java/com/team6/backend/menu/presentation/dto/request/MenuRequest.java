package com.team6.backend.menu.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MenuRequest {

    @Schema(description = "메뉴 이름", example = "된장찌개", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "메뉴 이름은 필수입니다.")
    private String name;

    @Schema(description = "가격", example = "6000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "가격은 필수입니다.")
    private Integer price;

    @Schema(description = "메뉴 설명", example = "추억 속의 그 맛")
    private String description;

    @Schema(description = "AI 설명 생성 여부", defaultValue = "false")
    private boolean aiDescription = false;

    @Schema(description = "AI 프롬프트 (aiDescription이 true일 때 필수)", example = "된장찌개 설명을 작성해줘")
    private String aiPrompt;

    @AssertTrue(message = "프롬프트를 입력해 주세요.")
    public boolean hasAiDescription() {
        if (aiDescription) {
            return aiPrompt != null && !aiPrompt.isBlank();
        }
        return true;
    }

}
