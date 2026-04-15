package com.clone.getchu.domain.trade.controller;

import com.clone.getchu.domain.trade.dto.request.TradeStatusUpdateRequest;
import com.clone.getchu.domain.trade.dto.response.GetTradeDetailResponse;
import com.clone.getchu.domain.trade.service.TradeService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // 상품 예약 요청
//    @PostMapping("/products/{productId}/reserve")
//    public ResponseEntity<ApiResponse<Void>> reserveProduct(
//            @PathVariable Long productId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//        tradeService.reserveProduct(productId, userDetails.getMemberId());
//        return ResponseEntity.ok(ApiResponse.success());
//    }

    /**
     * 거래 상태 변경
     * request.status() 에 따라 서비스 메서드를 분기합니다.
     *   - RESERVED : proceedTrade  (RESERVED → TRADING)
     *   - TRADING  : completeTrade (TRADING  → SOLD)
     *   - SALE     : cancelTrade   (현재 상태 → SALE 취소)
     */
    @PatchMapping("/trades/{tradeId}/status")
    public ResponseEntity<ApiResponse<Void>> changeTradeStatus(
            @PathVariable Long tradeId,
            @Valid @RequestBody TradeStatusUpdateRequest request) {

        switch (request.status()) {
            case RESERVED -> tradeService.proceedTrade(tradeId);
            case TRADING  -> tradeService.completeTrade(tradeId);
            case SALE     -> tradeService.cancelTrade(tradeId);
            default       -> throw new IllegalArgumentException("처리할 수 없는 status 값입니다: " + request.status());
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    //거래 상세 조회
    @GetMapping("/trades/{tradeId}")
    public ResponseEntity<ApiResponse<GetTradeDetailResponse>> getTradeDetail(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        GetTradeDetailResponse response = tradeService.getTradeDetail(tradeId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

