package com.team6.backend.menu.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.MenuErrorCode;
import com.team6.backend.global.infrastructure.util.AuthValidator;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.menu.presentation.dto.request.MenuRequest;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreService storeService;
    private final SecurityUtils securityUtils;
    private final AuthValidator authValidator;

    @Transactional
    public MenuResponse createMenu(UUID storeId, MenuRequest request) {
        Store store = storeService.findStoreById(storeId);
        authValidator.validateAccess(
                store.getOwnerId(),
                null,
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );

        String description;
        if (request.isAiDescription()) {
            // TODO: AI 요청 보내기
            description = "AI 설명";
        } else {
            description = request.getDescription();
        }

        Menu menu = new Menu(storeId, request.getName(), request.getPrice(), description);
        Menu saved = menuRepository.save(menu);
        return new MenuResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> getMenus(UUID storeId, String keyword, int page, int size, String sortBy, boolean isAsc) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Menu> menuList;
        if (keyword != null && !keyword.isBlank()) {
            menuList = menuRepository.findByStoreIdAndNameContainingIgnoreCase(storeId, keyword, pageable);
        } else {
            menuList = menuRepository.findByStoreId(storeId, pageable);
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
        Store store = storeService.findStoreById(menu.getStoreId());
        authValidator.validateAccess(
                store.getOwnerId(),
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
        Store store = storeService.findStoreById(menu.getStoreId());
        authValidator.validateAccess(
                store.getOwnerId(),
                List.of(Role.MASTER), // 무조건 허용
                List.of(Role.OWNER), // 조건부 허용
                MenuErrorCode.MENU_FORBIDDEN
        );
        menu.markDeleted(securityUtils.getCurrentUserId().toString());
    }

    @Transactional
    public MenuResponse hideMenu(UUID menuId) {
        Menu menu = findMenuById(menuId);
        Store store = storeService.findStoreById(menu.getStoreId());
        authValidator.validateAccess(
                store.getOwnerId(),
                List.of(Role.MASTER, Role.MANAGER),
                List.of(Role.OWNER),
                MenuErrorCode.MENU_FORBIDDEN
        );
        menu.hideMenu();
        return new MenuResponse(menu);
    }

    private Menu findMenuById(UUID menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new ApplicationException(MenuErrorCode.MENU_NOT_FOUND));
    }

}
