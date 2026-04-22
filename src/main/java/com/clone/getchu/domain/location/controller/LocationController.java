package com.clone.getchu.domain.location.controller;

import com.clone.getchu.domain.location.dto.request.AddressVerifyRequest;
import com.clone.getchu.domain.location.dto.request.GpsVerifyRequest;
import com.clone.getchu.domain.location.dto.response.LocationVerifyResponse;
import com.clone.getchu.domain.location.service.LocationService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    // GPS 좌표 기반 동네 인증
    @PostMapping("/verify/gps")
    public ResponseEntity<ApiResponse<LocationVerifyResponse>> verifyByGps(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GpsVerifyRequest request) {
        LocationVerifyResponse response =
                locationService.verifyByGps(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("동네 인증이 완료되었습니다.", response));
    }

    // 주소 텍스트 기반 동네 인증
    @PostMapping("/verify/address")
    public ResponseEntity<ApiResponse<LocationVerifyResponse>> verifyByAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressVerifyRequest request) {
        LocationVerifyResponse response =
                locationService.verifyByAddress(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("동네 인증이 완료되었습니다.", response));
    }
}
