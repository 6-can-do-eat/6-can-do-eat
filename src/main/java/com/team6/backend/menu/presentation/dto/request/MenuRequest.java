package com.team6.backend.menu.presentation.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MenuRequest {

    @NotBlank(message = "메뉴 이름은 필수입니다.")
    private String name;

    @NotNull(message = "가격은 필수입니다.")
    private int price;

    private String description;

    private boolean aiDescription = false;

    private String aiPrompt;

    @AssertTrue(message = "프롬프트를 입력해 주세요.")
    public boolean isAiDescription() {
        return aiDescription && aiPrompt != null;
    }

}
