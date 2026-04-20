package com.clone.getchu.domain.like.entity;

import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "likes",
    uniqueConstraints ={
            @UniqueConstraint(columnNames = {"product_id", "member_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE likes SET is_delete = ture WHERE id = ?") // 삭제 시 업데이트로 변경
@Where(clause = "is_deletee = false") // 조회 시 삭제 안 된 것만 필터링
public class Like extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private boolean isDeleted = false;

    @Builder
    public Like(Product product, Member member) {
        this.product = product;
        this.member = member;
    }

    public void restore() {
        this.isDeleted = false;
    }
}
