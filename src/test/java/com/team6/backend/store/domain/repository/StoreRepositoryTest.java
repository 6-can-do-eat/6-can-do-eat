package com.team6.backend.store.domain.repository;

import com.team6.backend.area.domain.entity.Area;
import com.team6.backend.area.domain.repository.AreaRepository;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.domain.repository.CategoryRepository;
import com.team6.backend.global.infrastructure.config.AuditorConfig;
import com.team6.backend.global.infrastructure.config.JpaAuditingConfig;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({JpaAuditingConfig.class, AuditorConfig.class})
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AreaRepository areaRepository;

    private User owner;
    private Category category;
    private Category category1;
    private Category category2;
    private Area area;
    private Area area1;
    private Area area2;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .username("ownerUser")
                .password("password")
                .role(Role.OWNER)
                .nickname("사장님")
                .build();
        userRepository.save(owner);

        category = new Category("치킨");
        categoryRepository.save(category);

        category1 = categoryRepository.save(new Category("치킨"));
        category2 = categoryRepository.save(new Category("피자"));

        area1 = areaRepository.save(new Area("강남구", "서울시", "강남구", true));
        area2 = areaRepository.save(new Area("서초구", "서울시", "서초구", true));

        area = new Area("강남구", "서울시", "강남구", true);
        areaRepository.save(area);
    }

    @Test
    @DisplayName("가게 검색 - 키워드 및 필터 적용 (숨김 처리된 가게 제외)")
    void searchStores() {
        // given
        Store store1 = new Store(owner, category, area, "맛있는 치킨집", "서울시 강남구");
        Store store2 = new Store(owner, category, area, "그냥 치킨집", "서울시 서초구");
        Store hiddenStore = new Store(owner, category, area, "비밀 치킨집", "서울시 강남구");
        hiddenStore.hideStore(); // 숨김 처리

        storeRepository.save(store1);
        storeRepository.save(store2);
        storeRepository.save(hiddenStore);

        Pageable pageable = PageRequest.of(0, 10);

        // when - 키워드 "맛있는"으로 검색
        Page<Store> result = storeRepository.searchStores("맛있는", category.getCategoryId(), area.getAreaId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("맛있는 치킨집");
        assertThat(result.getContent().get(0).isHidden()).isFalse();
    }

    @Test
    @DisplayName("사장님 ID로 가게 목록 검색")
    void searchStoresByOwnerId() {
        // given
        User otherOwner = User.builder()
                .username("otherOwner")
                .password("password")
                .role(Role.OWNER)
                .nickname("다른사장님")
                .build();
        userRepository.save(otherOwner);

        Store myStore = new Store(owner, category, area, "내 가게", "서울시 강남구");
        Store otherStore = new Store(otherOwner, category, area, "남의 가게", "서울시 강남구");

        storeRepository.save(myStore);
        storeRepository.save(otherStore);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Store> result = storeRepository.searchStoresByOwnerId(owner.getId(), null, null, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOwner().getId()).isEqualTo(owner.getId());
        assertThat(result.getContent().get(0).getName()).isEqualTo("내 가게");
    }

    @Test
    @DisplayName("가게 평점 업데이트 검증")
    void updateRating() {
        // given
        Store store = new Store(owner, category, area, "평점 맛집", "서울시 강남구");
        storeRepository.save(store);

        // when
        store.updateRating(4.567); // 반올림 logic 포함
        storeRepository.saveAndFlush(store);

        // then
        Store savedStore = storeRepository.findById(store.getStoreId()).orElseThrow();
        assertThat(savedStore.getRating()).isEqualTo(4.6);
    }

    @Test
    @DisplayName("가게 검색 실패 - 일치하는 키워드가 없는 경우 빈 페이지 반환")
    void searchStores_NoResult() {
        // given
        Store store = new Store(owner, category, area, "비비큐", "서울시 강남구");
        storeRepository.save(store);

        Pageable pageable = PageRequest.of(0, 10);

        // when - 존재하지 않는 이름 "교촌"으로 검색
        Page<Store> result = storeRepository.searchStores("교촌", null, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("가게 검색 실패 - 숨김 처리된 가게는 검색 결과에서 제외됨")
    void searchStores_ExcludeHidden() {
        // given
        Store hiddenStore = new Store(owner, category, area, "숨겨진 맛집", "서울시 강남구");
        hiddenStore.hideStore(); // isHidden = true 설정
        storeRepository.save(hiddenStore);

        Pageable pageable = PageRequest.of(0, 10);

        // when - searchStores는 Query에서 isHidden IS FALSE 조건을 가짐
        Page<Store> result = storeRepository.searchStores("숨겨진", null, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("가게 검색 실패 - 삭제된 가게(Soft Delete)는 조회되지 않음")
    void searchStores_ExcludeDeleted() {
        // given
        Store store = new Store(owner, category, area, "삭제될 가게", "서울시 강남구");
        storeRepository.save(store);

        store.markDeleted(String.valueOf(owner.getId()));
        storeRepository.saveAndFlush(store);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Store> result = storeRepository.searchStores("삭제될", null, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("저장 실패 - 필수 값(가게 이름)이 누락된 경우 예외 발생")
    void save_NullName_ThrowsException() {
        // given - @Column(nullable = false)인 name이 null
        Store invalidStore = new Store(owner, category, area, null, "서울시 강남구");

        // when & then
        assertThatThrownBy(() -> storeRepository.saveAndFlush(invalidStore))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("저장 실패 - 소유자(User) 정보가 누락된 경우 예외 발생")
    void save_NullOwner_ThrowsException() {
        // given - @JoinColumn(nullable = false)인 owner가 null
        Store invalidStore = new Store(null, category, area, "주인 없는 가게", "서울시 강남구");

        // when & then
        assertThatThrownBy(() -> storeRepository.saveAndFlush(invalidStore))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("사장님 전용 검색 실패 - 다른 사장님의 ID로 조회 시 내 가게가 나오지 않음")
    void searchStoresByOwnerId_WrongOwner() {
        // given
        Store myStore = new Store(owner, category, area, "내 가게", "서울시 강남구");
        storeRepository.save(myStore);

        UUID randomOwnerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Store> result = storeRepository.searchStoresByOwnerId(randomOwnerId, null, null, null, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("키워드가 빈 문자열(\"\")인 경우 모든 (숨겨지지 않은) 가게 조회")
    void searchStores_EmptyKeyword() {
        // given
        storeRepository.save(new Store(owner, category1, area1, "교촌치킨", "주소1"));
        storeRepository.save(new Store(owner, category2, area2, "도미노피자", "주소2"));

        // when - 키워드 "" 전달 (JPQL LIKE '%%' 동작)
        Page<Store> result = storeRepository.searchStores("", null, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("대소문자 구분 없이 검색어 일치 여부 확인")
    void searchStores_CaseInsensitive() {
        // given
        storeRepository.save(new Store(owner, category1, area1, "KFC Gangnam", "주소1"));

        // when - 소문자로 검색
        Page<Store> result = storeRepository.searchStores("kfc", null, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("KFC Gangnam");
    }

    @Test
    @DisplayName("복합 필터링: 키워드 + 카테고리 + 지역이 모두 일치해야 함")
    void searchStores_CombinedFilters() {
        // given
        storeRepository.save(new Store(owner, category1, area1, "강남치킨", "주소1")); // Match
        storeRepository.save(new Store(owner, category2, area1, "강남피자", "주소2")); // Category Mismatch
        storeRepository.save(new Store(owner, category1, area2, "서초치킨", "주소3")); // Area Mismatch

        // when
        Page<Store> result = storeRepository.searchStores("치킨", category1.getCategoryId(), area1.getAreaId(), PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("강남치킨");
    }

    @Test
    @DisplayName("페이지네이션: 결과가 페이징 크기를 초과할 때 다음 페이지 동작 확인")
    void searchStores_Pagination_NextPage() {
        // given - 3개의 가게 저장
        for (int i = 1; i <= 3; i++) {
            storeRepository.save(new Store(owner, category1, area1, "가게" + i, "주소" + i));
        }

        // when - 페이지 크기 2로 설정하고 1페이지(index 0) 요청
        Page<Store> firstPage = storeRepository.searchStores(null, null, null, PageRequest.of(0, 2));
        Page<Store> secondPage = storeRepository.searchStores(null, null, null, PageRequest.of(1, 2));

        // then
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("정렬: 평점 내림차순 정렬이 검색 쿼리에 적용되는지 확인")
    void searchStores_Sorting_Rating() {
        // given
        Store lowRating = new Store(owner, category1, area1, "낮은평점", "주소1");
        lowRating.updateRating(1.0);
        Store highRating = new Store(owner, category1, area1, "높은평점", "주소2");
        highRating.updateRating(5.0);

        storeRepository.save(lowRating);
        storeRepository.save(highRating);

        // when - rating 기준 DESC 정렬
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating"));
        Page<Store> result = storeRepository.searchStores(null, null, null, pageable);

        // then
        assertThat(result.getContent().get(0).getName()).isEqualTo("높은평점");
        assertThat(result.getContent().get(1).getName()).isEqualTo("낮은평점");
    }

    @Test
    @DisplayName("사장님 전용 검색: 사장님은 자신이 '숨김(isHidden)' 처리한 가게도 볼 수 있어야 함")
    void searchStoresByOwnerId_IncludeHidden() {
        // given
        Store hiddenStore = new Store(owner, category1, area1, "내 비밀가게", "주소1");
        hiddenStore.hideStore(); // isHidden = true
        storeRepository.save(hiddenStore);

        // when - searchStoresByOwnerId 쿼리는 isHidden 조건을 체크하지 않음
        Page<Store> result = storeRepository.searchStoresByOwnerId(owner.getId(), null, null, null, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).isHidden()).isTrue();
    }

    @Test
    @DisplayName("평점 업데이트 경계값: 0.0과 5.0 확인")
    void updateRating_Boundaries() {
        // given
        Store store = new Store(owner, category1, area1, "경계값가게", "주소1");
        storeRepository.save(store);

        // when & then (최소값)
        store.updateRating(0.0);
        assertThat(store.getRating()).isEqualTo(0.0);

        // when & then (최대값)
        store.updateRating(5.0);
        assertThat(store.getRating()).isEqualTo(5.0);
    }
}