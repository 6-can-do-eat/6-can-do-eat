package com.team6.backend.store.application.service;

import com.team6.backend.global.infrastructure.config.security.util.SecurityUtils;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.global.infrastructure.exception.StoreErrorCode;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.domain.repository.StoreRepository;
import com.team6.backend.store.presentation.dto.request.StoreRequest;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private StoreService storeService;

    // 테스트용 공통 데이터
    private final UUID ownerId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID areaId = UUID.randomUUID();

    private Store createMockStore(UUID ownerId) {
        Store store = new Store(ownerId, categoryId, areaId, "테스트 가게", "서울시 강남구");
        ReflectionTestUtils.setField(store, "StoreId", storeId);
        return store;
    }

    private StoreRequest createStoreRequest() {
        StoreRequest request = new StoreRequest();
        ReflectionTestUtils.setField(request, "name", "수정된 가게");
        ReflectionTestUtils.setField(request, "categoryId", categoryId);
        ReflectionTestUtils.setField(request, "areaId", areaId);
        ReflectionTestUtils.setField(request, "address", "수정된 주소");
        return request;
    }

    // ==========================================
    // 성공 케이스
    // ==========================================

    @Test
    @DisplayName("가게 생성 성공")
    void createStore_Success() {
        // given
        StoreRequest request = createStoreRequest();
        Store store = createMockStore(ownerId);

        given(securityUtils.getCurrentUserId()).willReturn(ownerId);
        given(storeRepository.save(any(Store.class))).willReturn(store);

        // when
        StoreResponse response = storeService.createStore(request);

        // then
        assertThat(response.getName()).isEqualTo("테스트 가게");
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    @DisplayName("가게 목록 조회 성공")
    void getStores_Success() {
        // given
        Store store = createMockStore(ownerId);
        Page<Store> storePage = new PageImpl<>(Collections.singletonList(store));
        given(storeRepository.searchStores(any(), any(), any(), any(Pageable.class))).willReturn(storePage);

        // when
        Page<StoreResponse> result = storeService.getStores("테스트", categoryId, areaId, 0, 10, "createdAt", false);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("테스트 가게");
    }

    @Test
    @DisplayName("가게 정보 수정 성공 - OWNER 본인 가게")
    void updateStore_Success() {
        // given
        Store store = createMockStore(ownerId);
        StoreRequest request = createStoreRequest();

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(ownerId);

        // when
        StoreResponse response = storeService.updateStore(storeId, request);

        // then
        assertThat(response.getName()).isEqualTo("수정된 가게");
    }

    @Test
    @DisplayName("가게 삭제 성공 - OWNER 본인 가게")
    void deleteStore_Success() {
        // given
        Store store = createMockStore(ownerId);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(ownerId);

        // when
        storeService.deleteStore(storeId);

        // then
        assertThat(store.isDeleted()).isTrue();
    }

    // ==========================================
    // 실패 케이스
    // ==========================================

    @Test
    @DisplayName("가게 상세 조회 실패 - 존재하지 않는 가게")
    void getStoreById_Fail_NotFound() {
        // given
        given(storeRepository.findById(storeId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getStoreById(storeId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(StoreErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("가게 수정 실패 - CUSTOMER 권한 접근 불가")
    void updateStore_Fail_Forbidden_Customer() {
        // given
        Store store = createMockStore(ownerId);
        StoreRequest request = createStoreRequest();

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(storeId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CommonErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("가게 수정 실패 - 다른 OWNER의 가게 접근")
    void updateStore_Fail_Forbidden_AnotherOwner() {
        // given
        Store store = createMockStore(ownerId);
        StoreRequest request = createStoreRequest();
        UUID anotherOwnerId = UUID.randomUUID();

        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.OWNER);
        given(securityUtils.getCurrentUserId()).willReturn(anotherOwnerId);

        // when & then
        assertThatThrownBy(() -> storeService.updateStore(storeId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CommonErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("가게 숨김 처리 실패 - 권한 없음")
    void hideStore_Fail_Forbidden() {
        // given
        Store store = createMockStore(ownerId);
        given(storeRepository.findById(storeId)).willReturn(Optional.of(store));
        given(securityUtils.getCurrentUserRole()).willReturn(Role.CUSTOMER);

        // when & then
        assertThatThrownBy(() -> storeService.hideStore(storeId))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(CommonErrorCode.FORBIDDEN.getMessage());
    }
}
