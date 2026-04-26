package com.team6.backend.address.presentation.cotrolller;

import com.team6.backend.address.application.service.AddressService;
import com.team6.backend.address.presentation.controlller.AddressController;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import com.team6.backend.global.infrastructure.exception.CommonErrorCode;
import com.team6.backend.user.domain.entity.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@Import(SecurityConfig.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private User user;
    private UserDetailsImpl userDetails;
    private UUID userId;
    private UUID adId;
    private AddressResponse mockResponse;

    @BeforeEach
    void setUp() throws Exception {
        // 1. JWT 필터 통과 모킹
        willAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).given(jwtFilter).doFilter(any(), any(), any());

        // 2. JwtAuthenticationEntryPoint 모킹 (401 Unauthorized 반환)
        willAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }).given(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        userId = UUID.randomUUID();
        adId = UUID.randomUUID();
        user = mock(User.class);
        given(user.getId()).willReturn(userId);
        userDetails = new UserDetailsImpl(user);

        AddressRequest mockRequest = new AddressRequest("서울시 강남구", "101호", false, "집");
        mockResponse = new AddressResponse(adId, mockRequest.getAddress(), mockRequest.getDetail(), mockRequest.isDefault(), mockRequest.getAlias());
    }

    // ==========================================
    // 성공 케이스
    // ==========================================

    @Test
    @DisplayName("배송지 생성 성공 - CUSTOMER 권한 (201 Created)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void createAddress_Success() throws Exception {
        AddressRequest request = new AddressRequest("서울시", "101호", false, "집");
        given(addressService.addAddress(any(AddressRequest.class), any(User.class))).willReturn(mockResponse);

        mockMvc.perform(post("/api/v1/addresses")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON_201"));
    }

    @Test
    @DisplayName("배송지 삭제 성공 - CUSTOMER 권한 (204 No Content)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void deleteAddress_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("배송지 삭제 성공 - MASTER 권한 (204 No Content)")
    @WithMockUser(username = "master1", roles = "MASTER")
    void deleteAddress_Master_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("내 배송지 목록 조회 성공 - CUSTOMER 권한 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getAddresses_Success() throws Exception {
        Page<AddressResponse> page = new PageImpl<>(Collections.singletonList(mockResponse));
        given(addressService.getAddress(any(User.class), any(), anyInt(), anyInt(), anyString(), anyBoolean()))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/users/{userId}/addresses", userId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"));
    }

    @Test
    @DisplayName("배송지 상세 조회 성공 - CUSTOMER 권한 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getAddressById_Success() throws Exception {
        given(addressService.getAddressById(any(UUID.class))).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"));
    }

    @Test
    @DisplayName("배송지 수정 성공 - CUSTOMER 권한 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateAddress_Success() throws Exception {
        AddressRequest request = new AddressRequest("수정된 서울시", "수정된 101호", true, "수정된 집");
        given(addressService.updateAddress(any(UUID.class), any(AddressRequest.class))).willReturn(mockResponse);

        mockMvc.perform(put("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"));
    }

    @Test
    @DisplayName("기본 배송지 설정 성공 - CUSTOMER 권한 (200 OK)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateDefaultAddress_Success() throws Exception {
        given(addressService.UpdateDefault(any(UUID.class))).willReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/addresses/{adId}/default", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON_200"));
    }

    // ==========================================
    // 실패 케이스
    // ==========================================

    @Test
    @DisplayName("배송지 생성 실패 - CUSTOMER 권한 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void createAddress_Fail_Forbidden_Customer() throws Exception {
        // AddressService에서 FORBIDDEN 예외 발생 시뮬레이션
        given(addressService.addAddress(any(AddressRequest.class), any(User.class)))
                .willThrow(new ApplicationException(CommonErrorCode.FORBIDDEN));

        AddressRequest request = new AddressRequest("서울시", "101호", false, "집");
        mockMvc.perform(post("/api/v1/addresses")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("배송지 삭제 실패 - 권한 없음 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void deleteAddress_Fail_Forbidden() throws Exception {
        // 서비스에서 FORBIDDEN 예외 발생 시뮬레이션
        doThrow(new ApplicationException(CommonErrorCode.FORBIDDEN))
                .when(addressService).deleteAddress(any(UUID.class));

        mockMvc.perform(delete("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 배송지 목록 조회 실패 - 다른 유저의 목록 요청 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getAddresses_Fail_Forbidden_OtherUser() throws Exception {
        UUID otherUserId = UUID.randomUUID(); // 다른 유저 ID
        mockMvc.perform(get("/api/v1/users/{userId}/addresses", otherUserId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("배송지 상세 조회 실패 - 존재하지 않는 ID (404 Not Found)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void getAddressById_Fail_NotFound() throws Exception {
        given(addressService.getAddressById(any(UUID.class)))
                .willThrow(new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("배송지 수정 실패 - 권한 없음 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateAddress_Fail_Forbidden() throws Exception {
        AddressRequest request = new AddressRequest("수정된 서울시", "수정된 101호", true, "수정된 집");
        given(addressService.updateAddress(any(UUID.class), any(AddressRequest.class)))
                .willThrow(new ApplicationException(CommonErrorCode.FORBIDDEN));

        mockMvc.perform(put("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("기본 배송지 설정 실패 - 권한 없음 (403 Forbidden)")
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void updateDefaultAddress_Fail_Forbidden() throws Exception {
        doThrow(new ApplicationException(CommonErrorCode.FORBIDDEN))
                .when(addressService).UpdateDefault(any(UUID.class));

        mockMvc.perform(patch("/api/v1/addresses/{adId}/default", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 실패: 토큰 없이 보호된 API 접근 시 (401 Unauthorized)")
    void accessWithoutToken_Fail() throws Exception {
        AddressRequest request = new AddressRequest("서울시", "101호", false, "집");
        mockMvc.perform(post("/api/v1/addresses")
                        .with(csrf()) // CSRF 토큰은 필요하지만, 인증 정보는 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
