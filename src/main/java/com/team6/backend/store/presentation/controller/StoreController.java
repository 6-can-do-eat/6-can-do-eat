package com.team6.backend.store.presentation.controller;

import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
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
    public ResponseEntity<SuccessResponse<StoreResponse>> createStore(@RequestBody StoreRequest request) {
        StoreResponse response = storeService.createStore(request);
        URI uri = URI.create("/api/v1/stores/" + response.getStoreId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "가게 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 가게 목록 조회 */
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<StoreResponse>>> getStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<StoreResponse> stores = storeService.getStores(keyword, categoryId, areaId, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(stores));
    }

    /* 가게 상세 조회 */
    @GetMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<StoreResponse>> getStoreById(@PathVariable UUID storeId) {
        StoreResponse response = storeService.getStoreById(storeId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 가게 정보 수정 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<StoreResponse>> updateStore(@PathVariable UUID storeId, @RequestBody StoreRequest request) {
        StoreResponse response = storeService.updateStore(storeId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "가게 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 가게 삭제 (소프트) */
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<SuccessResponse<Void>> deleteStore(@PathVariable UUID storeId) {
        storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build(); // 삭제 작업은 예외적으로 SuccessResponse 없이 상태 코드 204를 반환하는 것으로 결정되었습니다.
    }

    /* 가게 숨김 처리 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/{storeId}/hide")
    public ResponseEntity<SuccessResponse<StoreResponse>> hideStore(@PathVariable UUID storeId) {
        StoreResponse response = storeService.hideStore(storeId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "가게 숨김 변경이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

}
