package com.team6.backend.menu.presentation.dto.response;

import com.team6.backend.menu.domain.entity.Menu;
import lombok.Getter;

import java.util.UUID;

@Getter
public class MenuResponse {

    private final UUID menuId;
    private final UUID storeId;
    private final String name;
    private final int price;
    private final String description;
    private final boolean isHidden;

    public MenuResponse(Menu menu) {
        this.menuId = menu.getMenuId();
        this.storeId = menu.getStore().getStoreId();
        this.name = menu.getName();
        this.price = menu.getPrice();
        this.description = menu.getDescription();
        this.isHidden = menu.isHidden();
    }

}
