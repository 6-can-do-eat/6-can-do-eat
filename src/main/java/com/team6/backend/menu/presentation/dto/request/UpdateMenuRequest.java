package com.team6.backend.menu.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateMenuRequest {

    @Schema(description = "메뉴 이름", example = "된장찌개", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "메뉴 이름은 필수입니다.")
    private String name;

    @Schema(description = "가격", example = "6000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "가격은 필수입니다.")
    private Integer price;

    private String description;

}
