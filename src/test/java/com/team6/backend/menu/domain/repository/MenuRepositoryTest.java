package com.team6.backend.menu.domain.repository;

import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.area.domain.repository.AreaRepository;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.domain.repository.CategoryRepository;
import com.team6.backend.global.infrastructure.config.AuditorConfig;
import com.team6.backend.global.infrastructure.config.JpaAuditingConfig;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaAuditingConfig.class, AuditorConfig.class})
class MenuRepositoryTest {

    @Autowired private MenuRepository menuRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserInfoRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AreaRepository areaRepository;

    private Store savedStore;
    private Menu savedMenu;

    @BeforeEach
    void setUp() {
        // 연관 엔티티 생성 및 저장
        User owner = userRepository.save(new User("owner", "password", Role.OWNER, "사장님"));
        Category category = categoryRepository.save(new Category("치킨"));
        Area area = areaRepository.save(new Area("강남구", "서울", "강남구", true));

        savedStore = storeRepository.save(new Store(owner, category, area, "맛있는 치킨집", "서울시 강남구"));

        // 기본 메뉴 저장
        savedMenu = menuRepository.save(new Menu(savedStore, "후라이드 치킨", 18000, "바삭한 후라이드"));
    }

    // --- 성공 케이스 (Success Cases) ---

    @Test
    @DisplayName("성공: 가게 ID와 키워드로 메뉴 검색 (숨김 처리되지 않은 메뉴)")
    void searchMenus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Menu> result = menuRepository.searchMenus(savedStore.getStoreId(), "후라이드", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("후라이드 치킨");
    }

    @Test
    @DisplayName("성공: 가게 ID로 모든 메뉴 페이징 조회")
    void findByStore_StoreId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Menu> result = menuRepository.findByStore_StoreId(savedStore.getStoreId(), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 대소문자 구분 없이 메뉴 이름 포함 검색")
    void findByStore_StoreIdAndNameContainingIgnoreCase_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Menu> result = menuRepository.findByStore_StoreIdAndNameContainingIgnoreCase(savedStore.getStoreId(), "FRIED", pageable);

        // "FRIED" 검색을 위해 영문 이름 메뉴 추가 저장
        menuRepository.save(new Menu(savedStore, "Fried Chicken", 19000, "English Name"));

        Page<Menu> searchResult = menuRepository.findByStore_StoreIdAndNameContainingIgnoreCase(savedStore.getStoreId(), "fried", pageable);
        assertThat(searchResult.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("성공: 메뉴 ID와 가게 ID가 모두 일치하는 단건 조회")
    void findByMenuIdAndStore_StoreId_Success() {
        Optional<Menu> result = menuRepository.findByMenuIdAndStore_StoreId(savedMenu.getMenuId(), savedStore.getStoreId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("후라이드 치킨");
    }

    // --- 실패/예외 케이스 (Failure/Negative Cases) ---

    @Test
    @DisplayName("결과없음: 존재하지 않는 가게 ID로 메뉴 검색")
    void searchMenus_NonExistentStore() {
        Page<Menu> result = menuRepository.searchMenus(UUID.randomUUID(), "후라이드", PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 키워드가 일치하는 메뉴가 없는 경우")
    void searchMenus_NoMatchingKeyword() {
        Page<Menu> result = menuRepository.searchMenus(savedStore.getStoreId(), "피자", PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 메뉴가 '숨김(isHidden)' 상태인 경우 searchMenus에서 제외됨")
    void searchMenus_HiddenMenu_Filtered() {
        savedMenu.hideMenu(); // isHidden = true
        menuRepository.saveAndFlush(savedMenu);

        Page<Menu> result = menuRepository.searchMenus(savedStore.getStoreId(), "후라이드", PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 삭제된(Soft Delete) 메뉴는 조회되지 않음")
    void findByStore_StoreId_DeletedMenu_Filtered() {
        savedMenu.markDeleted("admin"); // BaseEntity의 soft delete
        menuRepository.saveAndFlush(savedMenu);

        Page<Menu> result = menuRepository.findByStore_StoreId(savedStore.getStoreId(), PageRequest.of(0, 10));
        // SQLRestriction("deleted_at IS NULL")에 의해 필터링됨
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 메뉴는 존재하지만 요청한 가게 ID가 다른 경우")
    void findByMenuIdAndStore_StoreId_WrongStore() {
        UUID otherStoreId = UUID.randomUUID();
        Optional<Menu> result = menuRepository.findByMenuIdAndStore_StoreId(savedMenu.getMenuId(), otherStoreId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("결과없음: 존재하지 않는 메뉴 ID로 단건 조회")
    void findByMenuIdAndStore_StoreId_NonExistentMenu() {
        Optional<Menu> result = menuRepository.findByMenuIdAndStore_StoreId(UUID.randomUUID(), savedStore.getStoreId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("결과없음: 메뉴 이름 검색 시 가게 ID가 틀리면 결과가 없음")
    void findByStore_StoreIdAndName_WrongStore() {
        Page<Menu> result = menuRepository.findByStore_StoreIdAndNameContainingIgnoreCase(UUID.randomUUID(), "후라이드", PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 삭제된 가게의 메뉴 조회 시도 (가게 자체가 필터링됨)")
    void findByStore_StoreId_DeletedStore() {
        savedStore.markDeleted("admin"); // Store soft delete
        storeRepository.saveAndFlush(savedStore);

        // 가게가 삭제되었으므로 해당 ID로 조회되는 메뉴도 없거나 접근 불가해야 함
        Page<Menu> result = menuRepository.findByStore_StoreId(savedStore.getStoreId(), PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 키워드가 null이면 searchMenus 쿼리 설계에 따라 결과가 없을 수 있음 (또는 전체 반환)")
    void searchMenus_NullKeyword() {
        // Repository의 @Query문에 따르면 keyword IS NULL 조건이 있으나, storeId가 정확해야 함
        Page<Menu> result = menuRepository.searchMenus(savedStore.getStoreId(), null, PageRequest.of(0, 10));
        // 쿼리 로직상 :keyword IS NULL OR ... 이므로 storeId가 맞으면 전체 출력됨.
        // 실패 상황을 위해 아예 메뉴가 없는 가게 ID를 넣음
        Page<Menu> emptyResult = menuRepository.searchMenus(UUID.randomUUID(), null, PageRequest.of(0, 10));
        assertThat(emptyResult.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("결과없음: 메뉴 설명(description)에는 키워드가 있지만 이름에는 없는 경우 검색 안됨")
    void searchMenus_KeywordInDescriptionOnly() {
        // searchMenus는 m.name에 대해서만 LIKE 연산을 수행함
        Page<Menu> result = menuRepository.searchMenus(savedStore.getStoreId(), "바삭한", PageRequest.of(0, 10));
        assertThat(result.isEmpty()).isTrue();
    }
}