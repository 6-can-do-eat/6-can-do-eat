package com.team6.backend.store.application.service;

import com.team6.backend.auth.domain.repository.UserRepository;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.exception.StoreErrorCode;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import com.team6.backend.user.domain.entity.User;
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
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public StoreResponse createStore(StoreRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        Store store = new Store(owner, request.getCategoryId(), request.getAreaId(), request.getName(), request.getAddress());
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
                .orElseThrow(() -> new ApplicationException(StoreErrorCode.STORE_NOT_FOUND));
    }
}
