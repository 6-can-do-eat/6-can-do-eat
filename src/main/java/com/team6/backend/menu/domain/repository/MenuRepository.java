package com.team6.backend.menu.domain.repository;

import com.team6.backend.menu.domain.entity.Menu;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    // TODO: QueryDsl 적용해야 함
    @Query("SELECT m FROM Menu m WHERE " +
            "(m.store.storeId = CAST(CAST(:storeId AS string) AS uuid)) AND " +
            "(:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
            "(m.isHidden IS FALSE)"
    )
    Page<Menu> searchMenus(
            @Param("storeId") UUID storeId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<Menu> findByStore_StoreId(UUID storeId, Pageable pageable);
    Page<Menu> findByStore_StoreIdAndNameContainingIgnoreCase(UUID storeId, String name, Pageable pageable);

    Optional<Menu> findByMenuIdAndStore_StoreId(UUID menuId, UUID storeId);
}
