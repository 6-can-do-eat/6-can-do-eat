package com.team6.backend.store.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class StoreRequest {

    @Schema(description = "가게 이름", example = "맛있는 한식당", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "가게 이름은 필수입니다.")
    private String name;

    @Schema(description = "카테고리 ID", example = "238ec5c7-bc05-48ac-924a-34dc306acc04", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "카테고리 ID는 필수입니다.")
    private UUID categoryId;

    @Schema(description = "지역 ID", example = "040b4329-7674-4e65-b767-a8e44adeeeae", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "지역 ID는 필수입니다.")
    private UUID areaId;

    @Schema(description = "가게 상세 주소", example = "서울시 종로구 광화문로 123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "가게 주소는 필수입니다.")
    private String address;

}
