package com.team6.backend.menu.domain.repository;

import com.team6.backend.menu.domain.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    Page<Menu> findByStore_StoreId(UUID storeId, Pageable pageable);
    Page<Menu> findByStore_StoreIdAndNameContainingIgnoreCase(UUID storeId, String name, Pageable pageable);

}
