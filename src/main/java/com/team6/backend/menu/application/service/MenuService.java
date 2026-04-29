package com.team6.backend.menu.application.service;

import com.team6.backend.ai.application.AiService;
import com.team6.backend.ai.presentation.dto.ProductDescriptionResponse;
import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.menu.domain.exception.MenuErrorCode;
import com.team6.backend.global.infrastructure.util.AuthValidator;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.menu.presentation.dto.request.MenuRequest;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.exception.StoreErrorCode;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final AiService aiService;
    private final StoreService storeService;
    private final SecurityUtils securityUtils;
    private final AuthValidator authValidator;

    @Transactional
    public MenuResponse createMenu(UUID storeId, MenuRequest request) {
        Store store = storeService.findStoreById(storeId);
        authValidator.validateAccess(
                store.getOwner().getId(),
                null,
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );

        String description;
        if (request.isAiDescription()) {
            ProductDescriptionResponse aiResponse = aiService.generateProductDescription(request.getAiPrompt());
            description = aiResponse.getResult();
        } else {
            description = request.getDescription();
        }

        Menu menu = new Menu(store, request.getName(), request.getPrice(), description);
        Menu saved = menuRepository.save(menu);
        return new MenuResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> getMenus(UUID storeId, String keyword, int page, int size, String sortBy, boolean isAsc) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        String safeKeyword = (keyword != null && !keyword.isBlank()) ? keyword : "";
        Page<Menu> menuList = menuRepository.searchMenus(storeId, safeKeyword, pageable);

        return menuList.map(MenuResponse::new);
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> getMenusForOwner(UUID storeId, String keyword, int page, int size, String sortBy, boolean isAsc) {
        Store store = storeService.findStoreById(storeId);
        authValidator.validateAccess(
                store.getOwner().getId(),
                null,
                List.of(Role.OWNER),
                StoreErrorCode.STORE_FORBIDDEN
        );

        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Menu> menuList;
        if (keyword != null && !keyword.isBlank()) {
            menuList = menuRepository.findByStore_StoreIdAndNameContainingIgnoreCase(storeId, keyword, pageable);
        } else {
            menuList = menuRepository.findByStore_StoreId(storeId, pageable);
        }

        return menuList.map(MenuResponse::new);
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenuById(UUID menuId) {
        Menu menu = findMenuById(menuId);
        return new MenuResponse(menu);
    }

    @Transactional
    public MenuResponse updateMenu(UUID menuId, UpdateMenuRequest request) {
        Menu menu = findMenuById(menuId);
        Store store = menu.getStore();
        authValidator.validateAccess(
                store.getOwner().getId(),
                List.of(Role.MASTER, Role.MANAGER),
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );
        menu.update(request);
        return new MenuResponse(menu);
    }

    @Transactional
    public void deleteMenu(UUID menuId) {
        Menu menu = findMenuById(menuId);
        Store store = menu.getStore();
        authValidator.validateAccess(
                store.getOwner().getId(),
                List.of(Role.MASTER),
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );
        menu.markDeleted(securityUtils.getCurrentUserId().toString());
    }

    @Transactional
    public MenuResponse hideMenu(UUID menuId) {
        Menu menu = findMenuById(menuId);
        Store store = menu.getStore();
        authValidator.validateAccess(
                store.getOwner().getId(),
                List.of(Role.MASTER, Role.MANAGER),
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );
        menu.hideMenu();
        return new MenuResponse(menu);
    }

    private Menu findMenuById(UUID menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> {
                    log.warn("[MENU] 메뉴를 찾을 수 없습니다. menuId: {}", menuId);
                    return new ApplicationException(MenuErrorCode.MENU_NOT_FOUND);
                });
    }

}
