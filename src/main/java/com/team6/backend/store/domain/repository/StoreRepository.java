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
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(:categoryId IS NULL OR s.category.categoryId = CAST(CAST(:categoryId AS string) AS uuid)) AND " +
            "(:areaId IS NULL OR s.area.areaId = CAST(CAST(:areaId AS string) AS uuid)) AND" +
            "(s.isHidden IS FALSE)"
    )
    Page<Store> searchStores(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("areaId") UUID areaId,
            Pageable pageable
    );

    // TODO: QueryDsl 적용해야 함
    @Query("SELECT s FROM Store s WHERE " +
            "(s.owner.id = CAST(CAST(:ownerId AS string) AS uuid)) AND " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(:categoryId IS NULL OR s.category.categoryId = CAST(CAST(:categoryId AS string) AS uuid)) AND " +
            "(:areaId IS NULL OR s.area.areaId = CAST(CAST(:areaId AS string) AS uuid))")
    Page<Store> searchStoresByOwnerId(
            @Param("ownerId") UUID ownerId,
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("areaId") UUID areaId,
            Pageable pageable
    );
}
