package com.team6.backend.review.presentation.controller;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.team6.backend.global.infrastructure.config.security.config.SecurityConfig;
import com.team6.backend.global.infrastructure.config.security.jwt.JwtFilter;
import com.team6.backend.review.application.service.ReviewService;
import com.team6.backend.review.presentation.dto.request.ReviewRequestDto;
import com.team6.backend.review.presentation.dto.response.ReviewResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        SecurityConfig.class,
                        JwtFilter.class
                })
        }
)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = new JsonMapper();

    @MockitoBean
    private ReviewService reviewService;

    private UUID orderId;
    private UUID reviewId;
    private UUID storeId;
    private ReviewRequestDto requestDto;
    private ReviewResponseDto responseDto;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        requestDto = new ReviewRequestDto();
        ReflectionTestUtils.setField(requestDto, "rating", 5);
        ReflectionTestUtils.setField(requestDto, "content", "아주 맛있습니다!");

        responseDto = new ReviewResponseDto();
        ReflectionTestUtils.setField(responseDto, "reviewId", reviewId);
    }

    @Test
    @DisplayName("리뷰 작성 API - 성공")
    @WithMockUser(roles = "CUSTOMER")
    void createReview() throws Exception {
        given(reviewService.createReview(eq(orderId), any(ReviewRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/api/v1/orders/{orderId}/review", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/reviews/" + reviewId))
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()));
    }

    @Test
    @DisplayName("리뷰 단일 조회 API - 성공")
    @WithMockUser
    void getReview() throws Exception {
        given(reviewService.getReview(reviewId)).willReturn(responseDto);

        mockMvc.perform(get("/api/v1/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()));
    }

    @Test
    @DisplayName("가게별 리뷰 목록 조회 API - 성공")
    @WithMockUser
    void getReviews() throws Exception {
        Page<ReviewResponseDto> pageResponse = new PageImpl<>(Collections.singletonList(responseDto));
        given(reviewService.getReviews(eq(storeId), anyInt(), anyInt(), anyString(), anyBoolean()))
                .willReturn(pageResponse);

        mockMvc.perform(get("/api/v1/stores/{storeId}/reviews", storeId)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("isAsc", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reviewId").value(reviewId.toString()));
    }

    @Test
    @DisplayName("리뷰 수정 API - 성공")
    @WithMockUser(roles = "CUSTOMER")
    void updateReview() throws Exception {
        given(reviewService.updateReview(eq(reviewId), any(ReviewRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(reviewId.toString()));
    }

    @Test
    @DisplayName("리뷰 삭제 API - 성공")
    @WithMockUser(roles = {"CUSTOMER", "MANAGER", "MASTER"})
    void deleteReview() throws Exception {
        doNothing().when(reviewService).deleteReview(reviewId);

        mockMvc.perform(delete("/api/v1/reviews/{reviewId}", reviewId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}