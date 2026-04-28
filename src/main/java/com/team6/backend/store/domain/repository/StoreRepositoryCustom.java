package com.team6.backend.store.domain.repository;

import com.team6.backend.store.domain.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StoreRepositoryCustom {

    Page<Store> searchStores(String keyword, UUID categoryId, UUID areaId, Pageable pageable);
    Page<Store> searchStoresByOwnerId(UUID ownerId, String keyword, UUID categoryId, UUID areaId, Pageable pageable);

}
