package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.clone\\.getchu\\.global\\.(security|config\\.SecurityConfig).*"
        )
)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @BeforeEach
    void setUp() {
        CustomUserDetails mockUser = new CustomUserDetails(1L, "test@test.com", "password", "USER");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_Success() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "아이폰 15 팝니다", "거의 새거에요", 1000000, 1L, List.of("url1", "url2")
        );
        ProductResponse response = new ProductResponse(
                1L,
                1L,
                "아이폰 15 팝니다",
                "거의 새거에요",
                1000000,
                ProductEnum.SALE,
                0,
                "디지털기기",
                "애플매니아",
                List.of("url1", "url2"),
                LocalDateTime.now()
        );

        // @AuthenticationPrincipal이 null이므로 any()로 매칭 (null 포함)
        given(productService.createProduct(any(ProductCreateRequest.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/products")
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
                1L, "제목", 10000, ProductEnum.SALE, "thumb.jpg", 0, LocalDateTime.now()
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
        ProductResponse response = new ProductResponse(
                1L,
                1L,
                "상세 제목",
                "상품 상세 설명입니다.",
                15000,
                ProductEnum.SALE,
                0,
                "전자기기",
                "판매자닉네임",
                List.of("https://image1.com", "https://image2.com"),
                LocalDateTime.now() // createdAt
        );
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
                "수정된 제목", null, 1200000, null, ProductEnum.RESERVED, null
        );
        ProductResponse response = new ProductResponse(
                1L,
                1L,
                "수정된 제목",
                "수정된 상세 설명입니다.",
                1200000,
                ProductEnum.RESERVED,
                0,
                "디지털기기",
                "판매자닉네임",
                List.of("https://image.com/new.jpg"),
                LocalDateTime.now()
        );

        given(productService.updateProduct(anyLong(), any(ProductUpdateRequest.class), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/products/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value(ProductEnum.RESERVED.name()));
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_Success() throws Exception {
        // given
        doNothing().when(productService).deleteProduct(anyLong(), any());

        // when & then
        mockMvc.perform(delete("/products/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 판매 목록 조회 성공 - /products/me")
    void getMyProducts_Success() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();

        // 1. DTO 구조에 맞춘 응답 데이터 생성
        ProductListResponse myProduct = new ProductListResponse(
                1L,
                "내가 파는 상품",
                50000,
                ProductEnum.SALE,
                "https://image.com/thumb1.jpg",
                0,
                now
        );

        CursorPageResponse<ProductListResponse> pageResponse = new CursorPageResponse<>(
                List.of(myProduct),
                now.toString() + "_1",
                false
        );

        given(productService.getMyProducts(any(), any(), any(), any(Pageable.class)))
                .willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/products/me")
                        .param("status", "SALE")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].title").value("내가 파는 상품"))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://image.com/thumb1.jpg"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }
}