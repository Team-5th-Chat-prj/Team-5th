package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.NearbyProductResponse;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Validated
public class NearbyProductController {

    private static final int PAGE_SIZE = 20;

    private final ProductService productService;

    /**
     * 내 동네 기반 근처 상품 목록 조회
     * GET /api/products/nearby?page=0
     *
     * 로그인한 회원의 인증된 위치(DB 저장값)를 기준으로 조회합니다.
     * 동네 인증이 되어 있지 않으면 400을 반환합니다.
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<Page<NearbyProductResponse>>> getNearbyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page) {

        Page<NearbyProductResponse> result = productService.getNearbyProducts(
                userDetails.getMemberId(), PageRequest.of(page, PAGE_SIZE));

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
