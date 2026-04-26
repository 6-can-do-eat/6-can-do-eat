package com.team6.backend.category.domain.repository;

import com.team6.backend.category.domain.entity.Category;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByName(@NotBlank(message = "카테고리 이름은 필수입니다.") String name);
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
