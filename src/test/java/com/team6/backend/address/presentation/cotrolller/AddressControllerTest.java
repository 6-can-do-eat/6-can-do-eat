package com.team6.backend.address.presentation.cotrolller;

import com.team6.backend.address.application.service.AddressService;
import com.team6.backend.address.presentation.controlller.AddressController;
import com.team6.backend.address.presentation.dto.AddressRequest;
import com.team6.backend.address.presentation.dto.AddressResponse;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthenticationEntryPoint;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
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
        willAnswer(invocation -> {
            invocation.getArgument(2, FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).given(jwtFilter).doFilter(any(), any(), any());

        userId = UUID.randomUUID();
        adId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testUser")
                .role(Role.CUSTOMER)
                .build();
        userDetails = new UserDetailsImpl(user);

        mockResponse = AddressResponse.builder()
                .adId(adId)
                .address("서울시")
                .detail("101호")
                .isDefault(false)
                .alias("집")
                .build();
    }

    @Test
    @DisplayName("배송지 생성 성공")
    @WithMockUser(roles = "CUSTOMER")
    void createAddress_Success() throws Exception {
        AddressRequest request = new AddressRequest("서울시", "101호", false, "집");
        given(addressService.addAddress(any(), any())).willReturn(mockResponse);

        mockMvc.perform(post("/api/v1/addresses")
                        .with(user(userDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("배송지 삭제 성공")
    @WithMockUser(roles = "CUSTOMER")
    void deleteAddress_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/addresses/{adId}", adId)
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
