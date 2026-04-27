package com.team6.backend.user.presentation.cotrolller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.backend.auth.presentation.dto.UserDetailsImpl;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtAuthUtils;
import com.team6.backend.global.infrastructure.redis.RedisService;
import com.team6.backend.user.application.service.UserService;
import com.team6.backend.user.domain.entity.Role;
import com.team6.backend.user.domain.entity.User;
import com.team6.backend.user.domain.repository.UserInfoRepository;
import com.team6.backend.user.presentation.dto.request.UserInfoRequest;
import com.team6.backend.user.presentation.dto.response.UserInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureAfter
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthUtils jwtAuthUtils;

    @MockitoBean
    private UserInfoRepository userInfoRepository;

    @MockitoBean
    private RedisService redisService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public JsonMapper jsonMapper() {
            // Jackson 3 스타일의 빌더 사용
            return JsonMapper.builder().build();
        }
    }

    private User testUser;
    private User masterUser;
    private UserDetailsImpl userDetails;
    private UserDetailsImpl masterDetails;
    private UserInfoResponse userInfoResponse;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        testUser = new User(UUID.randomUUID(), "testuser", "password", Role.CUSTOMER, "Nickname");
        masterUser = new User(UUID.randomUUID(), "master", "password", Role.MASTER, "MasterNickname");

        userDetails = new UserDetailsImpl(testUser);
        masterDetails = new UserDetailsImpl(masterUser);

        userInfoResponse = new UserInfoResponse(testUser.getId(), testUser.getUsername(), testUser.getNickname(), testUser.getRole(), LocalDateTime.now());
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    @WithMockUser
    void getUserDetail_success() throws Exception {
        given(userService.getUserDetail("testuser")).willReturn(userInfoResponse);

        mockMvc.perform(get("/api/v1/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("사용자 목록 조회 성공")
    @WithMockUser(roles = {"MASTER"})
    void getUsers_success() throws Exception {
        // PageRequest를 명시하여 Unpaged 오류 방지
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<UserInfoResponse> page = new PageImpl<>(Collections.singletonList(userInfoResponse), pageRequest, 1);
        given(userService.getUsers(any(), anyInt(), anyInt(), anyString(), anyBoolean())).willReturn(page);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("testuser"));
    }

    @Test
    @DisplayName("사용자 정보 수정 성공")
    void updateUser_success() throws Exception {
        UserInfoRequest request = new UserInfoRequest("testuser", "newPassword");
        given(userService.updateUser(eq("testuser"), any(UserInfoRequest.class), any(User.class))).willReturn(userInfoResponse);

        mockMvc.perform(put("/api/v1/users/testuser")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("정보 수정이 완료되었습니다."));
    }

    @Test
    @DisplayName("사용자 권한 변경 성공")
    void updateUserRole_success() throws Exception {
        UserInfoRequest request = new UserInfoRequest(null, null);
        ReflectionTestUtils.setField(request, "role", Role.MANAGER);

        given(userService.updateUserRole(eq("testuser"), eq(Role.MANAGER), any(User.class))).willReturn(userInfoResponse);

        mockMvc.perform(patch("/api/v1/users/testuser/role")
                        .with(csrf())
                        .with(user(masterDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("권한 변경이 완료되었습니다."));
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(eq("testuser"), any(User.class));

        mockMvc.perform(delete("/api/v1/users/testuser")
                        .with(csrf())
                        .with(user(masterDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자 삭제가 완료되었습니다."));
    }
}
