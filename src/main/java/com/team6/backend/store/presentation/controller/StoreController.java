package com.team6.backend.store.presentation.controller;

import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    /* 가게 생성 */
    public StoreResponse createStore(@RequestBody StoreRequest request) {
        return storeService.createStore(request);
    }

    /* 가게 목록 조회 */
    public Page<StoreResponse> getStores(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("isAsc") boolean isAsc
    ) {
        return storeService.getStores(page-1, size, sortBy, isAsc);
    }

    /* 가게 상세 조회 */
    @GetMapping("/{storeId}")
    public StoreResponse getStoreById(@PathVariable UUID storeId) {
        return storeService.getStoreById(storeId);
    }

    /* 가게 정보 수정 */
    @PutMapping("/{storeId}")
    public StoreResponse updateStore(@PathVariable UUID storeId, @RequestBody StoreRequest request) {
        return storeService.updateStore(storeId, request);
    }

    /* 가게 삭제 (소프트) */
    @DeleteMapping("/{storeId}")
    public void deleteStore(@PathVariable UUID storeId) {
        storeService.deleteStore(storeId);
    }

    /* 가게 숨김 처리 */
    @PatchMapping("/{storeId}/hide")
    public StoreResponse hideStore(@PathVariable UUID storeId) {
        return storeService.hideStore(storeId);
    }

}
