package com.clone.getchu.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * [로그인 요청 DTO]
 * POST /auth/login
 *
 * 로그인 실패 시 응답 분기:
 * - 이메일 없음  → 404 MEMBER_NOT_FOUND
 * - 비밀번호 불일치 → 401 INVALID_CREDENTIALS
 */
public record LoginRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
