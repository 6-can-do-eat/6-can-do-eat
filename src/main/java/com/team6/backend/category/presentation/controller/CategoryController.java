package com.team6.backend.category.presentation.controller;

import com.team6.backend.category.application.service.CategoryService;
import com.team6.backend.category.presentation.dto.request.CategoryRequest;
import com.team6.backend.category.presentation.dto.response.CategoryResponse;
import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Category", description = "카테고리 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /* 카테고리 생성 */
    @Operation(
            summary = "카테고리 생성",
            description = "새로운 카테고리를 등록합니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `MASTER`, `MANAGER`\n" +
                    "- 이름 중복: 이미 존재하는 카테고리 이름으로 생성 시 에러가 발생합니다.\n\n" +
                    "**[발생 가능한 비즈니스 에러]**\n" +
                    "- `CATEGORY_403`: 카테고리 생성 권한이 없음\n" +
                    "- `CATEGORY_409`: 이미 존재하는 카테고리 이름입니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "카테고리 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"카테고리 이름은 필수입니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_403\", \"status\": \"FORBIDDEN\", \"message\": \"카테고리에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "409", description = "이름 중복",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_409\", \"status\": \"CONFLICT\", \"message\": \"이미 존재하는 카테고리 이름입니다.\"}")))
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public ResponseEntity<SuccessResponse<CategoryResponse>> createCategory(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        URI uri = URI.create("/api/v1/categories/" + response.getCategoryId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "카테고리 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 카테고리 목록 조회 */
    @Operation(
            summary = "카테고리 목록 조회",
            description = "등록된 모든 카테고리를 페이징 처리하여 조회합니다. 키워드 검색 기능을 지원합니다.\n\n" +
                    "**[조회 특징]**\n" +
                    "- 삭제된 카테고리는 조회되지 않습니다.\n" +
                    "- 키워드가 없을 경우 전체 목록을 반환합니다.",
            parameters = {
                    @Parameter(name = "keyword", description = "검색어 (카테고리 이름 포함 여부)", example = "한식"),
                    @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
                    @Parameter(name = "size", description = "한 페이지당 노출 개수", example = "10"),
                    @Parameter(name = "sortBy", description = "정렬 기준 필드 (기본값: createdAt)", example = "name"),
                    @Parameter(name = "isAsc", description = "오름차순 여부 (false일 경우 내림차순)", example = "true")
            }
    )
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
    @Operation(
            summary = "카테고리 상세 조회",
            description = "카테고리 ID를 통해 특정 카테고리의 정보를 상세 조회합니다.\n\n" +
                    "**[비즈니스 에러]**\n" +
                    "- `CATEGORY_404`: 해당 ID의 카테고리가 존재하지 않거나 삭제됨",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "카테고리 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 카테고리가 존재하지 않습니다.\"}")))
            }
    )
    @GetMapping("/{categoryId}")
    public ResponseEntity<SuccessResponse<CategoryResponse>> getCategoryById(@PathVariable UUID categoryId) {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 카테고리 수정 */
    @Operation(
            summary = "카테고리 정보 수정",
            description = "기존 카테고리의 이름을 변경합니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `MASTER`, `MANAGER`\n" +
                    "- 중복 체크: 본인 이외의 다른 카테고리가 사용하는 이름으로는 수정할 수 없습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "입력값 검증 실패",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"COMMON_400\", \"status\": \"BAD_REQUEST\", \"message\": \"카테고리 이름은 필수입니다.\"}"))),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_403\", \"status\": \"FORBIDDEN\", \"message\": \"카테고리에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "카테고리 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 카테고리가 존재하지 않습니다.\"}"))),
                    @ApiResponse(responseCode = "409", description = "이름 중복",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_409\", \"status\": \"CONFLICT\", \"message\": \"이미 존재하는 카테고리 이름입니다.\"}")))
            }
    )
    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('MASTER', 'MANAGER')")
    public ResponseEntity<SuccessResponse<CategoryResponse>> updateCategory(@PathVariable UUID categoryId, @RequestBody @Valid CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "카테고리 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 카테고리 삭제 (소프트) */
    @Operation(
            summary = "카테고리 삭제 (소프트 삭제)",
            description = "카테고리를 시스템에서 삭제 처리합니다. 데이터는 보관되지만 일반 조회에서는 제외됩니다.\n\n" +
                    "**[권한 및 제약 사항]**\n" +
                    "- 권한: `MASTER`\n" +
                    "- 처리 방식: `deleted_at` 컬럼에 삭제 시간을 기록하고 삭제한 사용자 ID를 남깁니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 부족",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_403\", \"status\": \"FORBIDDEN\", \"message\": \"카테고리에 대한 권한이 없습니다.\"}"))),
                    @ApiResponse(responseCode = "404", description = "카테고리 없음",
                            content = @Content(examples = @ExampleObject(value = "{\"code\": \"CATEGORY_404\", \"status\": \"NOT_FOUND\", \"message\": \"해당 카테고리가 존재하지 않습니다.\"}")))
            }
    )
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('MASTER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

}