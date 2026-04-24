package com.team6.backend.area.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AreaCreateRequest {

    @NotBlank(message = "지역명은 필수입니다.")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "시/도는 필수입니다.")
    @Size(max = 50)
    private String city;

    @NotBlank(message = "구/군은 필수입니다.")
    @Size(max = 50)
    private String district;
}