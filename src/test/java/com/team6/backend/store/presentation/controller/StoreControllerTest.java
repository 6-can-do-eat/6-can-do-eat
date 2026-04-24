package com.team6.backend.store.presentation.controller;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.json.JsonMapper;
import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import com.team6.backend.store.application.service.StoreService;
import com.team6.backend.store.domain.entity.Store;
import com.team6.backend.store.presentation.dto.response.StoreResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreController.class)
@Import(SecurityConfig.class)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private StoreResponse mockResponse;
    private Map<String, Object> storeRequestMap;
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        // 모킹된 JwtFilter가 FilterChain을 끊지 않고 다음으로 넘기도록 설정
        willAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).given(jwtFilter).doFilter(any(), any(), any());

        // 테스트용 DTO 및 Response 초기화
        Store mockStore = new Store(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "테스트 가게", "서울시 종로구 광화문로 123");
        mockResponse = new StoreResponse(mockStore);

        storeRequestMap = new HashMap<>();
        storeRequestMap.put("name", "테스트 가게");
        storeRequestMap.put("categoryId", UUID.randomUUID().toString());
        storeRequestMap.put("areaId", UUID.randomUUID().toString());
        storeRequestMap.put("address", "서울시 종로구 광화문로 123");
    }

    @Test
    @DisplayName("가게 생성 - Owner1 성공 (201 Created)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createStore_ByOwner1_ShouldSucceed() throws Exception {
        given(storeService.createStore(any())).willReturn(mockResponse);

        mockMvc.perform(post("/api/v1/stores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(storeRequestMap)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("가게 생성 - Customer1 실패 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void createStore_ByCustomer1_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/stores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(storeRequestMap)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("가게 목록 조회 - Customer1 성공 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getStores_ByCustomer1_ShouldSucceed() throws Exception {
        given(storeService.getStores(any(), any(), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .willReturn(new PageImpl<>(Collections.singletonList(mockResponse)));

        mockMvc.perform(get("/api/v1/stores")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("isAsc", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.data.content[0].name").value("테스트 가게"));
    }

    @Test
    @DisplayName("가게 정보 수정 - Manager1 성공 (200 OK)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void updateStore_ByManager1_ShouldSucceed() throws Exception {
        given(storeService.updateStore(eq(storeId), any())).willReturn(mockResponse);

        mockMvc.perform(put("/api/v1/stores/{storeId}", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(storeRequestMap)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("가게 정보 수정 - Customer1 실패 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateStore_ByCustomer1_ShouldFail() throws Exception {
        mockMvc.perform(put("/api/v1/stores/{storeId}", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(storeRequestMap)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("가게 삭제 - Master1 성공 (204 No Content)")
    @WithMockUser(username = "master1", roles = "MASTER")
    void deleteStore_ByMaster1_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/stores/{storeId}", storeId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("가게 삭제 - Manager1 실패 (403 Forbidden)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void deleteStore_ByManager1_ShouldFail() throws Exception {
        mockMvc.perform(delete("/api/v1/stores/{storeId}", storeId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("가게 숨김 처리 - Owner2 성공 (200 OK)")
    @WithMockUser(username = "owner2", roles = "OWNER")
    void hideStore_ByOwner2_ShouldSucceed() throws Exception {
        given(storeService.hideStore(eq(storeId))).willReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/stores/{storeId}/hide", storeId)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("가게 생성 실패 - Validation 에러 (가게 이름 공백)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createStore_Fail_Validation_BlankName() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>(storeRequestMap);
        invalidRequest.put("name", "   "); // 공백

        mockMvc.perform(post("/api/v1/stores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("가게 이름은 필수입니다.")));
    }

    @Test
    @DisplayName("가게 생성 실패 - Validation 에러 (카테고리 ID 누락)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createStore_Fail_Validation_NullCategoryId() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>(storeRequestMap);
        invalidRequest.remove("categoryId"); // ID 누락

        mockMvc.perform(post("/api/v1/stores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("카테고리 ID는 필수입니다.")));
    }

    @Test
    @DisplayName("가게 생성 실패 - Validation 에러 (주소 공백)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createStore_Fail_Validation_BlankAddress() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>(storeRequestMap);
        invalidRequest.put("address", ""); // 공백 주소

        mockMvc.perform(post("/api/v1/stores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("가게 주소는 필수입니다.")));
    }

    @Test
    @DisplayName("가게 수정 실패 - Validation 에러 (지역 ID 누락)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void updateStore_Fail_Validation_NullAreaId() throws Exception {
        Map<String, Object> invalidRequest = new HashMap<>(storeRequestMap);
        invalidRequest.remove("areaId"); // 지역 ID 누락

        mockMvc.perform(put("/api/v1/stores/{storeId}", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("지역 ID는 필수입니다.")));
    }
}
