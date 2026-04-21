package com.clone.getchu.domain.member.entity;

import com.clone.getchu.domain.member.enums.MemberRole;
import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.math.RoundingMode;
@Entity
@Table(
        name = "members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
        }
)
@SQLRestriction("deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;
    // 회원 권한 (현재 USER 단일값, 추후 ADMIN 확장 가능)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.USER;

    // 회원 탈퇴 여부 (소프트 삭제)
    // true = 탈퇴한 회원, 목록/검색에서 제외
    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    private Member(String email, String password, String nickname, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl; // null이면 그대로 null
        this.averageRating = BigDecimal.ZERO;
        this.reviewCount = 0;
        this.role = MemberRole.USER;
        this.deleted = false;
    }

    // TODO [v2] Review 도메인 구현 후 연동 예정
    // - 리뷰 작성 시 updateReviewStats() 호출 / 거래 완료(SOLD) 상태 구매자만 작성 가능
    public void updateReviewStats(BigDecimal newRating) {
        // 새 평균 = (기존 평균 * 기존 리뷰 수 + 새 별점) / (기존 리뷰 수 + 1)
        // reviewCount는 계산 후 증가시키므로 현재 값이 곧 "기존 리뷰 수"
        BigDecimal newAvg = this.averageRating
                .multiply(BigDecimal.valueOf(this.reviewCount))
                .add(newRating)
                .divide(BigDecimal.valueOf(this.reviewCount + 1), 1, RoundingMode.HALF_UP);
        this.averageRating = newAvg;
        this.reviewCount++;
    }

    public void delete() {
        this.deleted = true;
    }

    public void update(String nickname, String profileImageUrl) {
        if (nickname != null) this.nickname = nickname;

        // profileImageUrl 처리:
        //   null        → 변경 의사 없음, 기존 값 유지
        //   ""(빈 문자열) → 프론트가 명시적으로 이미지 삭제 요청, null로 초기화
        //   URL 문자열   → 새 이미지 URL로 업데이트
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl.isEmpty() ? null : profileImageUrl;
        }
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
// TODO [v2] 거주지역 추가 예정
// - region 컬럼 추가 (예: "서울 강남구")
// - 동네 인증 기능 연동
// - 지역 기반 상품 검색 필터 적용
// private String region;

// TODO [v2] 회원 등급제 추가 예정
// - averageRating 기준으로 등급 계산
// - 🥕 새내기(1~2) / 😊 보통(3) / 😄 좋음(4) / 🌟 최고(5)
// - 프론트에서 배지 표시

// TODO [v2] 소프트 삭제 시 연관 데이터 처리 추가 예정
// - 등록 상품 비공개 처리 (Product.status → DELETED)
// - 진행중 거래 취소 처리 (Trade.status → CANCELLED)
