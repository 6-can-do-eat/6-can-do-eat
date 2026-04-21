package com.team6.backend.store.presentation.controller;

import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    /* 가게 생성 */
    @PostMapping
    @PreAuthorize("hasRole('OWNER')") // Role이 OWNER인 사용자만 호출 가능
    public ResponseEntity<StoreResponse> createStore(@RequestBody StoreRequest request) {
        StoreResponse response = storeService.createStore(request);
        URI uri = URI.create("/api/v1/stores/" + response.getStoreId());
        return ResponseEntity.created(uri).body(response);
    }

    /* 가게 목록 조회 */
    @GetMapping
    public ResponseEntity<Page<StoreResponse>> getStores(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("isAsc") boolean isAsc
    ) {
        return ResponseEntity.ok(storeService.getStores(page, size, sortBy, isAsc));
    }

    /* 가게 상세 조회 */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable UUID storeId) {
        return ResponseEntity.ok(storeService.getStoreById(storeId));
    }

    /* 가게 정보 수정 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/{storeId}")
    public ResponseEntity<StoreResponse> updateStore(@PathVariable UUID storeId, @RequestBody StoreRequest request) {
        return ResponseEntity.ok(storeService.updateStore(storeId, request));
    }

    /* 가게 삭제 (소프트) */
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(@PathVariable UUID storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build();
    }

    /* 가게 숨김 처리 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/{storeId}/hide")
    public ResponseEntity<StoreResponse> hideStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(storeService.hideStore(storeId));
    }

}
