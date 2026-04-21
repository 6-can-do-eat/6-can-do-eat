package com.team6.backend.store.presentation.dto.response;

import com.team6.backend.store.domain.entity.Store;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreResponse {

    private final UUID storeId;
    private final UUID categoryId;
    private final UUID areaId;
    private final String name;
    private final String address;
    private final boolean is_hidden;

    public StoreResponse(Store store) {
        this.storeId = store.getId();
        this.categoryId = store.getCategoryId();
        this.areaId = store.getAreaId();
        this.name = store.getName();
        this.address = store.getAddress();
        this.is_hidden = store.is_hidden();
    }

}
