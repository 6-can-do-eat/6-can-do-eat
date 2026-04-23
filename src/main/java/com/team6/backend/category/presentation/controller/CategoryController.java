package com.team6.backend.category.presentation.controller;

import com.team6.backend.category.application.service.CategoryService;
import com.team6.backend.category.presentation.dto.request.CategoryRequest;
import com.team6.backend.category.presentation.dto.response.CategoryResponse;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /* 카테고리 생성 */
    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public ResponseEntity<SuccessResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        URI uri = URI.create("/api/v1/categories/" + response.getCategoryId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "카테고리 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 카테고리 목록 조회 */
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<CategoryResponse>>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<CategoryResponse> categories = categoryService.getCategories(keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(categories));
    }

    /* 카테고리 상세 조회 */
    @GetMapping("/{categoryId}")
    public ResponseEntity<SuccessResponse<CategoryResponse>> getCategoryById(@PathVariable UUID categoryId) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 카테고리 수정 */
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public ResponseEntity<SuccessResponse<CategoryResponse>> updateCategory(@PathVariable UUID categoryId, @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "카테고리 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 카테고리 삭제 (소프트) */
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

}