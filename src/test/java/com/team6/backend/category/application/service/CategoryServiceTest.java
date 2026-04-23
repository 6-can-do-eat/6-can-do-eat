package com.team6.backend.category.application.service;

import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.domain.repository.CategoryRepository;
import com.team6.backend.category.presentation.dto.request.CategoryRequest;
import com.team6.backend.category.presentation.dto.response.CategoryResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CategoryErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequest createCategoryRequest(String name) {
        CategoryRequest request = new CategoryRequest();
        ReflectionTestUtils.setField(request, "name", name);
        return request;
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void createCategory_Success() {
        // given
        CategoryRequest request = createCategoryRequest("한식");
        Category category = new Category("한식");
        ReflectionTestUtils.setField(category, "categoryId", UUID.randomUUID());

        given(categoryRepository.existsByName("한식")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willReturn(category);

        // when
        CategoryResponse response = categoryService.createCategory(request);

        // then
        assertThat(response.getName()).isEqualTo("한식");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - 이름 중복")
    void createCategory_Fail_DuplicateName() {
        // given
        CategoryRequest request = createCategoryRequest("한식");
        given(categoryRepository.existsByName("한식")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CategoryErrorCode.DUPLICATE_CATEGORY_NAME.getMessage());
    }

    @Test
    @DisplayName("카테고리 목록 조회 성공")
    void getCategories_Success() {
        // given
        Category category = new Category("중식");
        Page<Category> categoryPage = new PageImpl<>(Collections.singletonList(category));
        given(categoryRepository.findAll(any(Pageable.class))).willReturn(categoryPage);

        // when
        Page<CategoryResponse> result = categoryService.getCategories(null, 0, 10, "createdAt", false);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("중식");
    }

    @Test
    @DisplayName("카테고리 상세 조회 성공")
    void getCategoryById_Success() {
        // given
        UUID categoryId = UUID.randomUUID();
        Category category = new Category("일식");
        ReflectionTestUtils.setField(category, "categoryId", categoryId);
        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

        // when
        CategoryResponse response = categoryService.getCategoryById(categoryId);

        // then
        assertThat(response.getCategoryId()).isEqualTo(categoryId);
        assertThat(response.getName()).isEqualTo("일식");
    }

    @Test
    @DisplayName("카테고리 수정 성공")
    void updateCategory_Success() {
        // given
        UUID categoryId = UUID.randomUUID();
        Category category = new Category("기존이름");
        CategoryRequest request = createCategoryRequest("새이름");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(categoryRepository.existsByName("새이름")).willReturn(false);

        // when
        CategoryResponse response = categoryService.updateCategory(categoryId, request);

        // then
        assertThat(response.getName()).isEqualTo("새이름");
    }

    @Test
    @DisplayName("카테고리 삭제 성공 (소프트 딜리트)")
    void deleteCategory_Success() {
        // given
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Category category = new Category("삭제할카테고리");

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
        given(securityUtils.getCurrentUserId()).willReturn(userId);

        // when
        categoryService.deleteCategory(categoryId);

        // then
        assertThat(category.isDeleted()).isTrue();
        assertThat(category.getDeletedBy()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("조회 결과 없음: 검색 키워드에 해당하는 카테고리가 없는 경우")
    void getCategories_EmptyResult() {
        given(categoryRepository.findByNameContainingIgnoreCase(anyString(), any()))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        Page<CategoryResponse> result = categoryService.getCategories("존재하지않음", 0, 10, "createdAt", false);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("상세 조회 실패: 존재하지 않는 UUID")
    void getCategoryById_NotFound() {
        UUID randomId = UUID.randomUUID();
        given(categoryRepository.findById(randomId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(randomId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("수정 성공: 자신의 현재 이름과 동일한 이름으로 수정 시 중복 오류 미발생")
    void updateCategory_SameName_Success() {
        UUID id = UUID.randomUUID();
        Category category = new Category("한식");
        CategoryRequest request = createCategoryRequest("한식");

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));
        // existsByName을 호출하지 않아야 함 (이름이 변경되지 않았으므로)

        CategoryResponse response = categoryService.updateCategory(id, request);

        assertThat(response.getName()).isEqualTo("한식");
    }

    @Test
    @DisplayName("수정 실패: 변경하려는 이름이 이미 다른 카테고리에서 사용 중")
    void updateCategory_Fail_NameAlreadyExists() {
        UUID id = UUID.randomUUID();
        Category category = new Category("기존이름");
        CategoryRequest request = createCategoryRequest("다른이름");

        given(categoryRepository.findById(id)).willReturn(Optional.of(category));
        given(categoryRepository.existsByName("다른이름")).willReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CategoryErrorCode.DUPLICATE_CATEGORY_NAME.getMessage());
    }

    @Test
    @DisplayName("삭제 실패: 이미 삭제된 카테고리를 다시 삭제 시도 (NotFound)")
    void deleteCategory_AlreadyDeleted() {
        // 프로젝트의 SQLRestriction("deleted_at IS NULL")에 의해 이미 삭제된 데이터는 findById에서 조회되지 않아야 함
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());
    }

}
