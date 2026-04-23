package com.team6.backend.menu.presentation.controller;

import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import com.team6.backend.menu.application.service.MenuService;
import com.team6.backend.menu.domain.entity.Menu;
import com.team6.backend.menu.presentation.dto.response.MenuResponse;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
@Import(SecurityConfig.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private MenuResponse mockResponse;
    private final UUID storeId = UUID.randomUUID();
    private final UUID menuId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        // 1. JWT 필터 통과 모킹 (SecurityContext를 건드리지 않고 다음 필터로 넘김)
        willAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).given(jwtFilter).doFilter(any(), any(), any());

        // 2. Menu 엔티티 생성 및 ID 강제 주입
        Menu mockMenu = new Menu(storeId, "테스트 메뉴", 10000, "테스트 설명");
        ReflectionTestUtils.setField(mockMenu, "menuId", menuId); // Null 방지

        // 3. Response DTO 초기화
        mockResponse = new MenuResponse(mockMenu);
    }

    // ==========================================
    // 성공 케이스
    // ==========================================

    @Test
    @DisplayName("메뉴 등록 성공 - OWNER 권한 (201 Created)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createMenu_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "테스트 메뉴");
        request.put("price", 10000);
        request.put("description", "테스트 설명");
        request.put("aiDescription", false);

        given(menuService.createMenu(eq(storeId), any())).willReturn(mockResponse);

        mockMvc.perform(post("/api/v1/stores/{storeId}/menus", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON_201"))
                .andExpect(jsonPath("$.data.menuId").value(menuId.toString()))
                .andExpect(jsonPath("$.data.name").value("테스트 메뉴"));
    }

    @Test
    @DisplayName("메뉴 목록 조회 성공 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getMenus_Success() throws Exception {
        given(menuService.getMenus(any(), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .willReturn(new PageImpl<>(Collections.singletonList(mockResponse)));

        mockMvc.perform(get("/api/v1/stores/{storeId}/menus", storeId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("isAsc", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.data.content[0].name").value("테스트 메뉴"));
    }

    @Test
    @DisplayName("메뉴 상세 조회 성공 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getMenuById_Success() throws Exception {
        given(menuService.getMenuById(menuId)).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/menus/{menuId}", menuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuId").value(menuId.toString()));
    }

    @Test
    @DisplayName("메뉴 수정 성공 - MANAGER 권한 (200 OK)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void updateMenu_Success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "수정된 메뉴");
        request.put("price", 22000);

        given(menuService.updateMenu(eq(menuId), any())).willReturn(mockResponse);

        mockMvc.perform(put("/api/v1/menus/{menuId}", menuId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("메뉴 정보 수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("메뉴 삭제 성공 - MASTER 권한 (204 No Content)")
    @WithMockUser(username = "master1", roles = "MASTER")
    void deleteMenu_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/menus/{menuId}", menuId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("메뉴 숨김 상태 변경 성공 (200 OK)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void hideMenu_Success() throws Exception {
        given(menuService.hideMenu(menuId)).willReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/menus/{menuId}/hide", menuId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("메뉴 숨김 변경이 완료되었습니다."));
    }

    // ==========================================
    // 실패 케이스
    // ==========================================

    @Test
    @DisplayName("메뉴 생성 실패 - Validation 에러 (메뉴명 누락)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createMenu_Fail_Validation_BlankName() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", ""); // 공백
        request.put("price", 10000);
        request.put("aiDescription", false);

        mockMvc.perform(post("/api/v1/stores/{storeId}/menus", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("메뉴 이름은 필수입니다.")));
    }

    @Test
    @DisplayName("메뉴 생성 실패 - Validation 에러 (AI 설정 true이나 프롬프트 누락)")
    @WithMockUser(username = "owner1", roles = "OWNER")
    void createMenu_Fail_Validation_AiPromptMissing() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "테스트 메뉴");
        request.put("price", 10000);
        request.put("aiDescription", true); // AI 활성화
        request.put("aiPrompt", null);      // 프롬프트 없음

        mockMvc.perform(post("/api/v1/stores/{storeId}/menus", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("프롬프트를 입력해 주세요.")));
    }

    @Test
    @DisplayName("메뉴 생성 실패 - CUSTOMER 권한은 접근 불가 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void createMenu_Fail_Forbidden_Customer() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "테스트 메뉴");
        request.put("price", 10000);
        request.put("aiDescription", false);

        mockMvc.perform(post("/api/v1/stores/{storeId}/menus", storeId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("메뉴 수정 실패 - CUSTOMER 권한은 접근 불가 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateMenu_Fail_Forbidden_Customer() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "수정된 메뉴");
        request.put("price", 12000);

        mockMvc.perform(put("/api/v1/menus/{menuId}", menuId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("메뉴 삭제 실패 - MANAGER 권한은 접근 불가 (403 Forbidden)")
    @WithMockUser(username = "manager1", roles = "MANAGER")
    void deleteMenu_Fail_Forbidden_Manager() throws Exception {
        mockMvc.perform(delete("/api/v1/menus/{menuId}", menuId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
