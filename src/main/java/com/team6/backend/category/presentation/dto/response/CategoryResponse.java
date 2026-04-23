package com.team6.backend.category.presentation.dto.response;

import com.team6.backend.category.domain.entity.Category;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryResponse {

    private final UUID categoryId;
    private final String name;

    public CategoryResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.name = category.getName();
    }

}
