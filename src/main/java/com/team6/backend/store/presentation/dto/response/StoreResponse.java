package com.team6.backend.store.presentation.dto.response;

import com.team6.backend.store.domain.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.UUID;

@Getter
public class StoreResponse {

    @Schema(description = "가게 ID", example = "ce3dde08-47da-4d44-b619-abd1f73f45ba")
    private final UUID storeId;

    @Schema(description = "카테고리 ID", example = "238ec5c7-bc05-48ac-924a-34dc306acc04")
    private final UUID categoryId;

    @Schema(description = "지역 ID", example = "040b4329-7674-4e65-b767-a8e44adeeeae")
    private final UUID areaId;

    @Schema(description = "가게 이름", example = "맛있는 한식당")
    private final String name;

    @Schema(description = "카테고리 이름", example = "한식")
    private final String categoryName;

    @Schema(description = "지역 이름", example = "광화문")
    private final String areaName;

    @Schema(description = "주소", example = "서울시 종로구 광화문로 123")
    private final String address;

    @Schema(description = "평균 평점", example = "4.5")
    private final double rating;

    @Schema(description = "숨김 여부", example = "false")
    private final boolean isHidden;

    public StoreResponse(Store store) {
        this.storeId = store.getStoreId();
        this.categoryId = store.getCategory().getCategoryId();
        this.areaId = store.getArea().getAreaId();
        this.name = store.getName();
        this.categoryName = store.getCategory().getName();
        this.areaName = store.getArea().getName();
        this.address = store.getAddress();
        this.rating = store.getRating();
        this.isHidden = store.isHidden();
    }

}
