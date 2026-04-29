package com.team6.backend.menu.domain.repository;

import com.team6.backend.menu.domain.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MenuRepositoryCustom {

    Page<Menu> searchMenus(UUID storeId, String keyword, Pageable pageable);
    Page<Menu> findByStore_StoreId(UUID storeId, Pageable pageable);
    Page<Menu> findByStore_StoreIdAndNameContainingIgnoreCase(UUID storeId, String name, Pageable pageable);
    Optional<Menu> findByMenuIdAndStore_StoreId(UUID menuId, UUID storeId);

}
