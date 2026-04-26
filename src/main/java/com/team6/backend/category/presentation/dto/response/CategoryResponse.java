package com.team6.backend.category.presentation.dto.response;

import com.team6.backend.category.domain.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryResponse {

    @Schema(description = "카테고리 ID", example = "238ec5c7-bc05-48ac-924a-34dc306acc04")
    private final UUID categoryId;

    @Schema(description = "카테고리 이름", example = "한식")
    private final String name;

    public CategoryResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.name = category.getName();
    }

}
