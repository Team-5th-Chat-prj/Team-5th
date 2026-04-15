package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import com.clone.getchu.global.security.JwtAuthFilter;
import com.clone.getchu.global.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)

class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private CustomUserDetails testUser;

    @BeforeEach
    void setUp() {
        // 테스트에서 공통으로 사용할 유저 정보 세팅
        testUser = new CustomUserDetails(1L, "test@test.com", "password", "USER");
    }

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_Success() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "아이폰 15 팝니다", "거의 새거에요", 1000000, 1L, List.of("url1", "url2")
        );
        ProductResponse response = new ProductResponse(1L, "아이폰 15 팝니다", "SALE");

        given(productService.createProduct(any(ProductCreateRequest.class), anyLong()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/products")
                        .with(csrf())
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.title").value("아이폰 15 팝니다"));
    }

    @Test
    @DisplayName("상품 목록 조회 성공")
    void getProducts_Success() throws Exception {
        // given
        ProductListResponse listResponse = new ProductListResponse(
                1L, "제목", 10000, "SALE", "thumb.jpg", LocalDateTime.now()
        );
        CursorPageResponse<ProductListResponse> pageResponse = new CursorPageResponse<>(
                List.of(listResponse), "next-cursor-123", false
        );

        given(productService.searchProducts(any(ProductSearchCondition.class), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/products")
                        .param("keyword", "아이폰")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].title").value("제목"));
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProduct_Success() throws Exception {
        // given
        ProductResponse response = new ProductResponse(1L, "상세 제목", "SALE");
        given(productService.getProduct(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.title").value("상세 제목"));
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_Success() throws Exception {
        // given
        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 제목", null, 1200000, null, "RESERVED", null
        );
        ProductResponse response = new ProductResponse(1L, "수정된 제목", "RESERVED");

        given(productService.updateProduct(anyLong(), any(ProductUpdateRequest.class), anyLong()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/products/{productId}", 1L)
                        .with(csrf())
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("RESERVED")); // status 필드 검증으로 수정
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_Success() throws Exception {
        // given
        doNothing().when(productService).deleteProduct(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/products/{productId}", 1L)
                        .with(csrf())
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("상품이 삭제되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist()); // NON_NULL 설정으로 인해 데이터가 없음
    }

    @Test
    @DisplayName("상품 생성 실패 - 유효성 검사 (제목 2자 미만)")
    void createProduct_Fail_Validation() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "아", // @Size(min = 2) 위반
                "설명", 1000, 1L, null
        );

        // when & then
        mockMvc.perform(post("/products")
                        .with(csrf())
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}