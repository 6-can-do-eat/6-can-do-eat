package com.team6.backend.area.presentation.dto.response;

import com.team6.backend.area.domain.entity.Area;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class AreaResponse {

    private final UUID areaId;
    private final String name;
    private final String city;
    private final String district;
    private final boolean is_active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public AreaResponse(Area area) {
        this.areaId = area.getAreaId();
        this.name = area.getName();
        this.city = area.getCity();
        this.district = area.getDistrict();
        this.is_active = area.is_active();
        this.createdAt = area.getCreatedAt();
        this.updatedAt = area.getUpdatedAt();
    }
}