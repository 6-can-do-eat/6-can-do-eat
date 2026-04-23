package com.team6.backend.menu.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.MenuErrorCode;
import com.team6.backend.global.infrastructure.exception.StoreErrorCode;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.domain.repository.MenuRepository;
import com.team6.backend.menu.presentation.dto.request.MenuRequest;
import com.team6.backend.menu.presentation.dto.request.UpdateMenuRequest;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private StoreService storeService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MenuService menuService;

    // ==========================================
    // 성공 케이스
    // ==========================================

    @Test
    @DisplayName("메뉴 생성 성공")
    void createMenu_Success() {
        // given
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID(); // 현재 사용자 ID이자 가게 주인 ID
        Store store = new Store(userId, UUID.randomUUID(), UUID.randomUUID(), "테스트 가게", "주소");

        MenuRequest request = new MenuRequest();
        ReflectionTestUtils.setField(request, "name", "후라이드 치킨");
        ReflectionTestUtils.setField(request, "price", 18000);
        ReflectionTestUtils.setField(request, "description", "바삭바삭");
        ReflectionTestUtils.setField(request, "aiDescription", false);

        Menu savedMenu = new Menu(storeId, "후라이드 치킨", 18000, "바삭바삭");
        ReflectionTestUtils.setField(savedMenu, "menuId", UUID.randomUUID());

        // Mock 설정
        given(storeService.findStoreById(storeId)).willReturn(store);
        given(securityUtils.getCurrentUserId()).willReturn(userId);
        given(menuRepository.save(any(Menu.class))).willReturn(savedMenu);

        // when
        MenuResponse response = menuService.createMenu(storeId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("후라이드 치킨");
        assertThat(response.getPrice()).isEqualTo(18000);
    }

    @Test
    @DisplayName("메뉴 목록 조회 성공 (키워드 없음)")
    void getMenus_Success_NoKeyword() {
        // given
        UUID storeId = UUID.randomUUID();
        Menu menu = new Menu(storeId, "치킨", 18000, "설명");
        ReflectionTestUtils.setField(menu, "menuId", UUID.randomUUID());
        Page<Menu> menuPage = new PageImpl<>(Collections.singletonList(menu));

        given(menuRepository.findByStoreId(eq(storeId), any(Pageable.class))).willReturn(menuPage);

        // when
        Page<MenuResponse> responses = menuService.getMenus(storeId, null, 0, 10, "createdAt", false);

        // then
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).getName()).isEqualTo("치킨");
    }

    @Test
    @DisplayName("메뉴 상세 조회 성공")
    void getMenuById_Success() {
        // given
        UUID menuId = UUID.randomUUID();
        Menu menu = new Menu(UUID.randomUUID(), "치킨", 18000, "설명");
        ReflectionTestUtils.setField(menu, "menuId", menuId);

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));

        // when
        MenuResponse response = menuService.getMenuById(menuId);

        // then
        assertThat(response.getMenuId()).isEqualTo(menuId);
        assertThat(response.getName()).isEqualTo("치킨");
    }

    @Test
    @DisplayName("메뉴 수정 성공 - OWNER 본인 가게")
    void updateMenu_Success() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Menu menu = new Menu(storeId, "기존 치킨", 18000, "기존 설명");
        ReflectionTestUtils.setField(menu, "menuId", menuId);
        Store store = new Store(ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게", "주소");

        UpdateMenuRequest request = new UpdateMenuRequest();
        ReflectionTestUtils.setField(request, "name", "수정된 치킨");
        ReflectionTestUtils.setField(request, "price", 20000);
        ReflectionTestUtils.setField(request, "description", "수정된 설명");

        // 권한 체크 Mocking (OWNER + 본인가게)
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(storeService.findStoreById(storeId)).willReturn(store);
        given(securityUtils.getCurrentUserId()).willReturn(ownerId);

        // when
        MenuResponse response = menuService.updateMenu(menuId, request);

        // then
        assertThat(response.getName()).isEqualTo("수정된 치킨");
        assertThat(response.getPrice()).isEqualTo(20000);
        assertThat(menu.getName()).isEqualTo("수정된 치킨"); // 엔티티 수정 확인
    }

    @Test
    @DisplayName("메뉴 삭제 성공 (소프트 딜리트)")
    void deleteMenu_Success() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Menu menu = new Menu(storeId, "치킨", 18000, "설명");
        Store store = new Store(ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게", "주소");

        // 권한 체크 Mocking (OWNER + 본인가게)
        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(storeService.findStoreById(storeId)).willReturn(store);
        given(securityUtils.getCurrentUserId()).willReturn(ownerId);

        // when
        menuService.deleteMenu(menuId);

        // then
        assertThat(menu.isDeleted()).isTrue();
        assertThat(menu.getDeletedBy()).isEqualTo(ownerId.toString());
    }

    @Test
    @DisplayName("메뉴 숨김 상태 변경 성공")
    void hideMenu_Success() {
        // given
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Menu menu = new Menu(storeId, "치킨", 18000, "설명");
        Store store = new Store(ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게", "주소");

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(storeService.findStoreById(storeId)).willReturn(store);
        given(securityUtils.getCurrentUserId()).willReturn(ownerId);

        // isHidden 초기값은 false
        assertThat(menu.isHidden()).isFalse();

        // when
        MenuResponse response = menuService.hideMenu(menuId);

        // then
        assertThat(response.isHidden()).isTrue();
        assertThat(menu.isHidden()).isTrue(); // 엔티티 상태 변경 확인
    }

    // ==========================================
    // 실패 케이스
    // ==========================================

    @Test
    @DisplayName("메뉴 생성 실패 - 존재하지 않는 가게")
    void createMenu_Fail_StoreNotFound() {
        UUID storeId = UUID.randomUUID();
        MenuRequest request = new MenuRequest();

        given(storeService.findStoreById(storeId))
                .willThrow(new ApplicationException(StoreErrorCode.STORE_NOT_FOUND));

        assertThatThrownBy(() -> menuService.createMenu(storeId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(StoreErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메뉴 수정 실패 - 존재하지 않는 메뉴")
    void updateMenu_Fail_MenuNotFound() {
        UUID menuId = UUID.randomUUID();
        UpdateMenuRequest request = new UpdateMenuRequest();

        given(menuRepository.findById(menuId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.updateMenu(menuId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MenuErrorCode.MENU_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("메뉴 수정 실패 - CUSTOMER 권한은 접근 불가")
    void updateMenu_Fail_Forbidden_Customer() {
        UUID menuId = UUID.randomUUID();
        Menu menu = new Menu(UUID.randomUUID(), "메뉴", 1000, "설명");
        UpdateMenuRequest request = new UpdateMenuRequest();

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        assertThatThrownBy(() -> menuService.updateMenu(menuId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MenuErrorCode.MENU_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("메뉴 수정 실패 - 본인 소유가 아닌 가게 (OWNER)")
    void updateMenu_Fail_Forbidden_NotStoreOwner() {
        UUID menuId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID(); // 다른 OWNER

        Menu menu = new Menu(storeId, "메뉴", 10000, "설명");
        Store store = new Store(ownerId, UUID.randomUUID(), UUID.randomUUID(), "가게", "주소");
        UpdateMenuRequest request = new UpdateMenuRequest();

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(storeService.findStoreById(storeId)).willReturn(store);
        given(securityUtils.getCurrentUserId()).willReturn(anotherUserId);

        assertThatThrownBy(() -> menuService.updateMenu(menuId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MenuErrorCode.MENU_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("메뉴 삭제 실패 - MANAGER 권한은 삭제 불가")
    void deleteMenu_Fail_Forbidden_Manager() {
        UUID menuId = UUID.randomUUID();
        Menu menu = new Menu(UUID.randomUUID(), "메뉴", 10000, "설명");

        given(menuRepository.findById(menuId)).willReturn(Optional.of(menu));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.MANAGER);

        assertThatThrownBy(() -> menuService.deleteMenu(menuId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(MenuErrorCode.MENU_FORBIDDEN.getMessage());
    }
}
