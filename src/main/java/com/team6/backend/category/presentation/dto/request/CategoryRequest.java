package com.team6.backend.category.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CategoryRequest {

    @Schema(description = "카테고리 이름", example = "한식", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String name;

}
