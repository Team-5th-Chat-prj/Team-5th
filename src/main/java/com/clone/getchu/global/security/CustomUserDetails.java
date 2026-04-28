package com.clone.getchu.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * [Spring Security 인증 주체] JWT 클레임을 Security가 이해하는 형식으로 래핑
 * <p>
 * Spring Security는 인증된 사용자 정보를 UserDetails 인터페이스로 다룹니다.
 * DB를 조회하지 않고 JWT 클레임(memberId, email, role)만으로 객체를 구성합니다.
 * <p>
 * 사용 흐름:
 * 1. JwtProvider.getAuthentication() 에서 JWT 파싱 후 이 객체 생성
 * 2. JwtAuthFilter가 SecurityContext에 저장
 * 3. 컨트롤러에서 @AuthenticationPrincipal CustomUserDetails userDetails 로 꺼냄
 * 또는 SecurityUtil.getCurrentMemberId() 로 memberId만 꺼냄
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long memberId;  // DB PK, 비즈니스 로직에서 주로 사용
    private final String email;// JWT subject 필드
    private final String nickname;
    private final String role;    // 예: "USER", "ADMIN" (DB에 저장된 값 그대로)

    /**
     * Spring Security 권한 반환
     * role이 "USER"이면 → "ROLE_USER" 로 변환해서 반환
     * <p>
     * [주의사항]
     * - DB에 "USER"로 저장 → "ROLE_USER" 자동 변환 (정상)
     * - DB에 "ROLE_USER"로 저장 → "ROLE_ROLE_USER" 이중 접두사 문제 발생
     * → Member 엔티티의 role 컬럼에는 반드시 "ROLE_" 없이 저장할 것
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        // return Collections.singletonList 보다 List.of 가 코드가 더 짧고 직관적이며 null 값을 허용하지않아 더 안전
    }

    /**
     * JWT 방식에서는 비밀번호 검증이 필요 없으므로 null 반환
     * 실제 비밀번호 검증은 로그인 시 BCrypt로 처리
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Security에서 "사용자 이름"으로 쓰이는 필드
     * 이 프로젝트에서는 email을 식별자로 사용
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * 아래 4개는 계정 상태 체크 메서드
     * Stateless JWT 방식에서는 모두 true로 고정
     * 계정 잠금/만료 같은 상태 관리는 JWT 자체 만료(exp 클레임)로 대신함
     * <p>
     * [추후 확장 포인트]
     * - 계정 정지 기능 추가 시 isEnabled()에서 Member.isActive 필드 체크
     * - 비밀번호 변경 후 기존 토큰 무효화 필요 시 isCredentialsNonExpired() 활용
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}