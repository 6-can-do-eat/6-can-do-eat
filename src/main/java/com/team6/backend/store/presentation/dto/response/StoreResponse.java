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
    private final boolean isHidden;

    public StoreResponse(Store store) {
        this.storeId = store.getStoreId();
        this.categoryId = store.getCategory().getCategoryId();
        this.areaId = store.getArea().getAreaId();
        this.name = store.getName();
        this.address = store.getAddress();
        this.isHidden = store.isHidden();
    }

}
