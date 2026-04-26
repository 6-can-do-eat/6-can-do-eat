package com.team6.backend.menu.presentation.dto.response;

import com.team6.backend.menu.domain.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.UUID;

@Getter
public class MenuResponse {

    @Schema(description = "메뉴 ID", example = "eb8f4339-ddb4-4173-af2b-fb4ce75fbed4")
    private final UUID menuId;

    @Schema(description = "가게 ID", example = "ce3dde08-47da-4d44-b619-abd1f73f45ba")
    private final UUID storeId;

    @Schema(description = "가게 이름", example = "맛있는 한식당")
    private final String storeName;

    @Schema(description = "메뉴 이름", example = "된장찌개")
    private final String name;

    @Schema(description = "가격", example = "6000")
    private final int price;

    @Schema(description = "메뉴 설명", example = "추억 속의 그 맛")
    private final String description;

    @Schema(description = "숨김 여부", example = "false")
    private final boolean isHidden;

    public MenuResponse(Menu menu) {
        this.menuId = menu.getMenuId();
        this.storeId = menu.getStore().getStoreId();
        this.storeName = menu.getStore().getName();
        this.name = menu.getName();
        this.price = menu.getPrice();
        this.description = menu.getDescription();
        this.isHidden = menu.isHidden();
    }

}
