package com.clone.getchu.domain.review.controller;

import com.clone.getchu.domain.review.dto.request.ReviewRequest;
import com.clone.getchu.domain.review.dto.response.ReviewResponse;
import com.clone.getchu.domain.review.service.ReviewService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated  // @Max, @Positive 등 메서드 파라미터 제약 활성화
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 작성 — 거래 완료(SOLD) 상태, 구매자 본인만 가능
    @PostMapping("/trades/{tradeId}/reviews")
    public ResponseEntity<ApiResponse<Void>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tradeId,
            @Valid @RequestBody ReviewRequest request) {
        reviewService.createReview(userDetails, tradeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    // 리뷰 목록 조회 — 비로그인 가능, 커서 기반 페이지네이션
    // size 최대 50으로 제한 — 악의적 대량 조회 방지
    @GetMapping("/members/{memberId}/reviews")
    public ResponseEntity<ApiResponse<CursorPageResponse<ReviewResponse>>> getReviews(
            @PathVariable Long memberId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Positive @Max(50) int size) {
        CursorPageResponse<ReviewResponse> response = reviewService.getReviews(memberId, cursor, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
