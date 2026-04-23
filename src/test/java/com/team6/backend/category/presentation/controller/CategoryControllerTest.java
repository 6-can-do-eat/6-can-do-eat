package com.team6.backend.category.presentation.controller;

import com.team6.backend.category.application.service.CategoryService;
import com.team6.backend.category.domain.entity.Category;
import com.team6.backend.category.presentation.dto.response.CategoryResponse;
import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private CategoryResponse mockResponse;
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        willAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).given(jwtFilter).doFilter(any(), any(), any());

        Category mockCategory = new Category("테스트 카테고리");
        ReflectionTestUtils.setField(mockCategory, "categoryId", categoryId);
        mockResponse = new CategoryResponse(mockCategory);
    }

    @Test
    @DisplayName("카테고리 생성 성공 - MASTER 권한 (201 Created)")
    @WithMockUser(roles = "MASTER")
    void createCategory_ByMaster_ShouldSucceed() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "신규 카테고리");

        given(categoryService.createCategory(any())).willReturn(mockResponse);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON_201"));
    }

    @Test
    @DisplayName("카테고리 생성 실패 - CUSTOMER 권한 (403 Forbidden)")
    @WithMockUser(roles = "CUSTOMER")
    void createCategory_ByCustomer_ShouldFail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "권한없는 카테고리");

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카테고리 목록 조회 - 누구나 성공 (200 OK)")
    @WithMockUser(roles = "CUSTOMER")
    void getCategories_ShouldSucceed() throws Exception {
        given(categoryService.getCategories(any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .willReturn(new PageImpl<>(Collections.singletonList(mockResponse)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("테스트 카테고리"));
    }

    @Test
    @DisplayName("카테고리 수정 성공 - MANAGER 권한 (200 OK)")
    @WithMockUser(roles = "MANAGER")
    void updateCategory_ByManager_ShouldSucceed() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "수정된 카테고리");

        given(categoryService.updateCategory(eq(categoryId), any())).willReturn(mockResponse);

        mockMvc.perform(put("/api/v1/categories/{categoryId}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("카테고리 삭제 성공 - MASTER 권한 (204 No Content)")
    @WithMockUser(roles = "MASTER")
    void deleteCategory_ByMaster_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - MANAGER 권한 (403 Forbidden)")
    @WithMockUser(roles = "MANAGER")
    void deleteCategory_ByManager_ShouldFail() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{categoryId}", categoryId)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("생성 실패: 카테고리 이름이 공백인 경우 (400 Bad Request)")
    @WithMockUser(roles = "MASTER")
    void createCategory_BlankName_Fail() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("name", "  "); // 공백 문자열

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("수정 실패: OWNER 권한은 카테고리 수정 불가 (403 Forbidden)")
    @WithMockUser(roles = "OWNER")
    void updateCategory_ByOwner_Fail() throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, String> request = new HashMap<>();
        request.put("name", "수정시도");

        mockMvc.perform(put("/api/v1/categories/{categoryId}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("삭제 실패: MANAGER 권한은 카테고리 삭제 불가 (403 Forbidden)")
    @WithMockUser(roles = "MANAGER")
    void deleteCategory_ByManager_Fail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/categories/{categoryId}", id)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 실패: 토큰 없이 접근 시 (401 Unauthorized)")
    void accessWithoutToken_Fail() throws Exception {
        // @WithMockUser 없이 수행
        mockMvc.perform(get("/api/v1/categories/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("조회 실패: 잘못된 형식의 UUID 경로 변수")
    @WithMockUser(roles = "CUSTOMER")
    void getCategory_InvalidUUIDFormat() throws Exception {
        mockMvc.perform(get("/api/v1/categories/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

}
