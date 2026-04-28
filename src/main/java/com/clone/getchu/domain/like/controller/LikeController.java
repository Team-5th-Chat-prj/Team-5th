package com.clone.getchu.domain.like.controller;

import com.clone.getchu.domain.like.service.LikeFacade;
import com.clone.getchu.domain.like.service.LikeService;
import com.clone.getchu.domain.product.dto.ProductListResponse;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;
    private final LikeFacade likeFacade;

    //찜하기
    @PostMapping("/products/{productId}/likes")
    public ResponseEntity<ApiResponse<Void>> createLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        likeFacade.createLike(productId, userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    //찜 취소
    @DeleteMapping("/products/{productId}/likes")
    public ResponseEntity<ApiResponse<Void>> deleteLike(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        likeFacade.deleteLike(productId, userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    //내 찜 목록 조회
    @GetMapping("members/me/likes")
    public ResponseEntity<ApiResponse<Page<ProductListResponse>>> myLikes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ProductListResponse> responses = likeService.getMyLikesList(userDetails.getMemberId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
