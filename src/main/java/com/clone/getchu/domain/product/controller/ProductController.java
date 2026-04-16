package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProductResponse response = productService.createProduct(request, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 전체 목록 조회 및 검색을 하나로 통합
     * GET /products?title=아이폰&categoryId=1...
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<ProductListResponse>>> getProducts(
            ProductSearchCondition cond,
           @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(cond, pageable)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProductResponse response = productService.updateProduct(productId, request, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        productService.deleteProduct(productId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}