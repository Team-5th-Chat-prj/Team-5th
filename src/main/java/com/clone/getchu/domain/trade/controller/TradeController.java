package com.clone.getchu.domain.trade.controller;

import com.clone.getchu.domain.trade.dto.request.TradeStatusUpdateRequest;
import com.clone.getchu.domain.trade.dto.response.GetAllTradeResponse;
import com.clone.getchu.domain.trade.dto.response.GetTradeDetailResponse;
import com.clone.getchu.domain.trade.service.TradeService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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


    @PatchMapping("/trades/{tradeId}/status")
    public ResponseEntity<ApiResponse<Void>> changeTradeStatus(
            @PathVariable Long tradeId,
            @Valid @RequestBody TradeStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        tradeService.updateTradeStatus(tradeId, request.status(), userDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.success());
    }

    //거래 상세 조회
    @GetMapping("/trades/{tradeId}")
    public ResponseEntity<ApiResponse<GetTradeDetailResponse>> getTradeDetail(
            @PathVariable Long tradeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        GetTradeDetailResponse response = tradeService.getTradeDetail(tradeId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //자신의 거래 리스트 조회
    @GetMapping("/members/me/trades")
    public ResponseEntity<ApiResponse<List<GetAllTradeResponse>>> getMyTrades(
            @RequestParam(name = "role") String role,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        List<GetAllTradeResponse> responses = tradeService.getMyTrade(userDetails.getMemberId(), role);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}

