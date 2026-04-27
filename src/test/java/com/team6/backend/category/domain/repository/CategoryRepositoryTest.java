package com.team6.backend.category.domain.repository;

import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.global.infrastructure.config.AuditorConfig;
import com.team6.backend.global.infrastructure.config.JpaAuditingConfig;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, AuditorConfig.class})
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("카테고리 저장 및 ID로 조회 성공")
    void saveAndFindById_success() {
        Category category = new Category("한식");
        Category saved = categoryRepository.save(category);

        Optional<Category> found = categoryRepository.findById(saved.getCategoryId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("한식");
    }

    @Test
    @DisplayName("존재하는 이름으로 existsByName 조회 시 true 반환")
    void existsByName_true_success() {
        categoryRepository.save(new Category("중식"));

        boolean exists = categoryRepository.existsByName("중식");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이름으로 existsByName 조회 시 false 반환")
    void existsByName_false_success() {
        boolean exists = categoryRepository.existsByName("없는카테고리");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("이름 키워드 포함 검색 성공 (대소문자 무시)")
    void findByNameContainingIgnoreCase_success() {
        categoryRepository.save(new Category("FastFood"));
        categoryRepository.save(new Category("K-Food"));

        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("food", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("name").containsExactlyInAnyOrder("FastFood", "K-Food");
    }

    @Test
    @DisplayName("검색 키워드와 일치하는 결과가 없으면 빈 페이지 반환")
    void findByName_noMatch_returnsEmptyPage() {
        categoryRepository.save(new Category("일식"));

        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("양식", PageRequest.of(0, 10));

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("소프트 델리트: 삭제된 카테고리는 findById로 조회되지 않음")
    void softDelete_findById_returnsEmpty() {
        Category category = categoryRepository.save(new Category("치킨"));
        category.markDeleted("admin"); // BaseEntity의 기능
        entityManager.flush();
        entityManager.clear();

        Optional<Category> found = categoryRepository.findById(category.getCategoryId());

        assertThat(found).isEmpty(); // @SQLRestriction에 의해 필터링됨
    }

    @Test
    @DisplayName("소프트 델리트: 삭제된 카테고리 이름은 existsByName에서 false 반환")
    void softDelete_existsByName_returnsFalse() {
        Category category = categoryRepository.save(new Category("피자"));
        category.markDeleted("user");
        entityManager.flush();

        boolean exists = categoryRepository.existsByName("피자");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("소프트 델리트: 삭제된 카테고리는 키워드 검색 결과에서 제외")
    void softDelete_search_excluded() {
        categoryRepository.save(new Category("분식집"));
        Category deleted = categoryRepository.save(new Category("철회된분식"));
        deleted.markDeleted("system");
        entityManager.flush();

        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("분식", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("분식집");
    }

    @Test
    @DisplayName("제약 조건 위반: 카테고리 이름이 null이면 저장 실패")
    void save_nullName_throwsException() {
        // given
        Category category = new Category(null);

        // when & then
        assertThatThrownBy(() -> {
            categoryRepository.save(category);
            entityManager.flush();
        }).isInstanceOfAny(
                ConstraintViolationException.class,
                DataIntegrityViolationException.class
        );
    }

    @Test
    @DisplayName("제약 조건 위반: 카테고리 이름이 50자를 초과하면 저장 실패")
    void save_tooLongName_throwsException() {
        // given
        String longName = "a".repeat(51);
        Category category = new Category(longName);

        // when & then
        assertThatThrownBy(() -> {
            categoryRepository.save(category);
            entityManager.flush();
        }).isInstanceOfAny(
                DataException.class,
                DataIntegrityViolationException.class
        );
    }

    @Test
    @DisplayName("경계값: 검색 키워드가 빈 문자열인 경우 전체 목록 반환")
    void search_emptyKeyword_returnsAll() {
        categoryRepository.save(new Category("A"));
        categoryRepository.save(new Category("B"));

        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이징: 데이터 범위를 벗어난 페이지 요청 시 빈 결과 반환")
    void pagination_outOfBounds_returnsEmpty() {
        categoryRepository.save(new Category("테스트"));

        Page<Category> result = categoryRepository.findAll(PageRequest.of(5, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 UUID로 조회 시 Empty 반환")
    void findById_nonExistUuid_returnsEmpty() {
        Optional<Category> result = categoryRepository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("공백 이름 존재 여부 확인: 존재하지 않으므로 false 반환")
    void existsByName_blankName_returnsFalse() {
        // @NotBlank validation은 Service 계층이나 Controller에서 주로 처리되지만
        // Repository 수준에서의 데이터 존재 여부만 테스트
        boolean exists = categoryRepository.existsByName("   ");

        assertThat(exists).isFalse();
    }
}