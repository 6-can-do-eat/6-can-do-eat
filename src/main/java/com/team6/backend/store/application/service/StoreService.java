package com.team6.backend.store.application.service;

import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        // TODO: User(Owner) 정보 가져와야 함
        Store store = new Store(null, request.getCategoryId(), request.getAreaId(), request.getName(), request.getAddress());
        Store saved =  storeRepository.save(store);
        return new StoreResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> getStores(int page, int size, String sortBy, boolean isAsc) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Store> storeList = storeRepository.findAll(pageable);

        return storeList.map(StoreResponse::new);
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(UUID storeId) {
        Store store = findStoreById(storeId);
        return new StoreResponse(store);
    }

    @Transactional
    public StoreResponse updateStore(UUID storeId, StoreRequest request) {
        Store store = findStoreById(storeId);
        store.update(request.getCategoryId(), request.getAreaId(), request.getName(), request.getAddress());
        return new StoreResponse(store);
    }

    @Transactional
    public void deleteStore(UUID storeId) {
        Store store = findStoreById(storeId);
        storeRepository.delete(store);
    }

    @Transactional
    public StoreResponse hideStore(UUID storeId) {
        Store store = findStoreById(storeId);
        store.hideStore();
        return new StoreResponse(store);
    }

    private Store findStoreById(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 가게가 존재하지 않습니다. storeId =" + storeId));
    }
}
