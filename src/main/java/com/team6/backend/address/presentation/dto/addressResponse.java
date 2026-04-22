package com.team6.backend.address.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class addressResponse {

    private final UUID adId;
    private final String address;
    private final String detail;
    private final boolean isDefault;

}
