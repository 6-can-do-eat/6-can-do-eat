package com.team6.backend.menu.presentation.controller;

import com.team6.backend.global.infrastructure.response.CommonSuccessCode;
import com.team6.backend.global.infrastructure.response.SuccessResponse;
import com.team6.backend.menu.application.service.MenuService;
import com.team6.backend.menu.presentation.dto.request.MenuRequest;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MenuController {

    private final MenuService menuService;

    /* 메뉴 등록 (AI 설명 생성 옵션) */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/stores/{storeId}/menus")
    public ResponseEntity<SuccessResponse<MenuResponse>> createMenu(@PathVariable UUID storeId, @RequestBody @Valid MenuRequest request) {
        MenuResponse response = menuService.createMenu(storeId, request);
        URI uri = URI.create("/api/v1/menus/" + response.getMenuId());
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.CREATED, "메뉴 생성이 완료되었습니다.", response);
        return ResponseEntity.created(uri).body(successResponse);
    }

    /* 메뉴 목록 조회 */
    @GetMapping("/stores/{storeId}/menus")
    public ResponseEntity<SuccessResponse<Page<MenuResponse>>> getMenus(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
            ) {
        Page<MenuResponse> menus = menuService.getMenus(storeId, keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(menus));
    }

    /* 메뉴 목록 조회 (본인 가게 메뉴 조회) */
    @GetMapping("/stores/{storeId}/menus/my")
    public ResponseEntity<SuccessResponse<Page<MenuResponse>>> getMenusForOwner(
            @PathVariable UUID storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "false") boolean isAsc
    ) {
        Page<MenuResponse> menus = menuService.getMenusForOwner(storeId, keyword, page, size, sortBy, isAsc);
        return ResponseEntity.ok(SuccessResponse.ok(menus));
    }

    /* 메뉴 상세 조회 */
    @GetMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<MenuResponse>> getMenuById(@PathVariable UUID menuId) {
        MenuResponse response = menuService.getMenuById(menuId);
        return ResponseEntity.ok(SuccessResponse.ok(response));
    }

    /* 메뉴 수정 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PutMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<MenuResponse>> updateMenu(@PathVariable UUID menuId, @RequestBody @Valid UpdateMenuRequest request) {
        MenuResponse response = menuService.updateMenu(menuId, request);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "메뉴 정보 수정이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

    /* 메뉴 삭제 (소프트) */
    @PreAuthorize("hasAnyRole('OWNER', 'MASTER')")
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<SuccessResponse<Void>> deleteMenu(@PathVariable UUID menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    /* 메뉴 숨김 처리 */
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'MASTER')")
    @PatchMapping("/menus/{menuId}/hide")
    public ResponseEntity<SuccessResponse<MenuResponse>> hideMenu(@PathVariable UUID menuId) {
        MenuResponse response = menuService.hideMenu(menuId);
        SuccessResponse successResponse = SuccessResponse.of(CommonSuccessCode.OK, "메뉴 숨김 변경이 완료되었습니다.", response);
        return ResponseEntity.ok(successResponse);
    }

}
