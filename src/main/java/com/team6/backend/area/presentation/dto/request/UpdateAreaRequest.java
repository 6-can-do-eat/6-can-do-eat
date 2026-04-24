package com.team6.backend.area.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateAreaRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String city;

    @Size(max = 50)
    private String district;

    private Boolean is_active;
}