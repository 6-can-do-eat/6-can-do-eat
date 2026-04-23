package com.team6.backend.store.domain.repository;

import com.team6.backend.store.domain.entity.Store;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    // TODO: QueryDsl 적용해야 함
    @Query("SELECT s FROM Store s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR s.categoryId = :categoryId) AND " +
            "(:areaId IS NULL OR s.areaId = :areaId)")
    Page<Store> searchStores(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("areaId") UUID areaId,
            Pageable pageable);
}
