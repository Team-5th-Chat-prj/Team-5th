package com.clone.getchu.domain.product.controller;

import com.clone.getchu.domain.product.dto.NearbyProductResponse;
import com.clone.getchu.domain.product.service.ProductService;
import com.clone.getchu.global.common.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Validated  // @RequestParam 레벨 제약 조건 활성화
public class NearbyProductController {

    private static final int PAGE_SIZE = 20;

    private final ProductService productService;

    /**
     * 근처 상품 목록 조회
     * GET /api/products/nearby?lat=37.5&lng=126.9&radius=3&page=0
     *
     * @param lat    위도 (-90.0 ~ 90.0)
     * @param lng    경도 (-180.0 ~ 180.0)
     * @param radius 반경 km (기본값 3, 최대 10)
     * @param page   페이지 번호 (0부터 시작)
     */
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<Page<NearbyProductResponse>>> getNearbyProducts(
            @RequestParam
            @NotNull(message = "위도(lat)는 필수입니다.")
            @DecimalMin(value = "-90.0",  message = "위도는 -90.0 이상이어야 합니다.")
            @DecimalMax(value = "90.0",   message = "위도는 90.0 이하이어야 합니다.")
            Double lat,

            @RequestParam
            @NotNull(message = "경도(lng)는 필수입니다.")
            @DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
            @DecimalMax(value = "180.0",  message = "경도는 180.0 이하이어야 합니다.")
            Double lng,

            @RequestParam(defaultValue = "3")
            @Min(value = 1,  message = "반경은 1km 이상이어야 합니다.")
            @Max(value = 10, message = "반경은 최대 10km까지 가능합니다.")
            int radius,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page) {

        Page<NearbyProductResponse> result = productService.getNearbyProducts(
                lat, lng, radius, PageRequest.of(page, PAGE_SIZE));

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
