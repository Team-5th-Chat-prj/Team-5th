package com.clone.getchu.domain.member.entity;

import com.clone.getchu.domain.member.enums.MemberRole;
import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    // 리뷰 평균 별점 (비정규화)
    // 리뷰 작성 시 단일 트랜잭션 내에서 함께 UPDATE
    // DECIMAL(2,1): 부동소수점 오차 방지, 소수점 1자리 반올림 저장
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating = BigDecimal.ZERO;
    @Column(nullable = false)
    private int reviewCount = 0;

    // 회원 권한 (현재 USER 단일값, 추후 ADMIN 확장 가능)
    // DB에 "USER", "ADMIN" 형태로 저장 (ROLE_ 접두사 없이)
    // CustomUserDetails에서 "ROLE_" + role 형태로 변환
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.USER;

    // 회원 탈퇴 여부 (소프트 삭제)
    // true = 탈퇴한 회원, 목록/검색에서 제외
    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    private Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.averageRating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.role = MemberRole.USER;
        this.deleted = false;
    }

    // TODO Review 도메인 추가 구현 예정
    public void updateReviewStats(int newRating) {
        // 새 평균=(기존 평균 * 기존 리뷰 수 + 새 별점) / (기존 리뷰 수 + 1)
        BigDecimal newAvg = this.averageRating
                .multiply(BigDecimal.valueOf(this.reviewCount))
                .add(BigDecimal.valueOf(newRating))
                .divide(BigDecimal.valueOf(this.reviewCount + 1), 1, RoundingMode.HALF_UP);
        this.averageRating = newAvg;
        this.reviewCount++;
    }

}
