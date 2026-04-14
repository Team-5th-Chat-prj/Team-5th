package com.clone.getchu.domain.member.controller;

import com.clone.getchu.domain.member.dto.reqeust.MemberUpdateRequest;
import com.clone.getchu.domain.member.dto.reqeust.UpdatePasswordRequest;
import com.clone.getchu.domain.member.dto.response.MemberProfileResponse;
import com.clone.getchu.domain.member.dto.response.MemberResponse;
import com.clone.getchu.domain.member.service.MemberService;
import com.clone.getchu.global.common.ApiResponse;
import com.clone.getchu.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    // 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyUserDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberResponse response = memberService.getMyInfo(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 회원 정보 수정(닉네임, 프로필 이미지)
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MemberUpdateRequest request) {
        MemberResponse response = memberService.update(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.delete(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 완료되었습니다.", null));
    }

    // 회원 비밀번호 변경
    @PatchMapping("me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {
        memberService.updatePassword(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    // 비로그인도 가능 상대방 정보 조회
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getMemberProfile(
            @PathVariable Long memberId) {
        return ResponseEntity.ok(
                ApiResponse.success(memberService.getMemberProfile(memberId))
        );
    }
}
