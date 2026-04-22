package com.team6.backend.store.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class StoreRequest {

    @NotBlank(message = "가게 이름은 필수입니다.")
    private String name;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private UUID categoryId;

    @NotNull(message = "지역 ID는 필수입니다.")
    private UUID areaId;

    @NotBlank(message = "가게 주소는 필수입니다.")
    private String address;

}
