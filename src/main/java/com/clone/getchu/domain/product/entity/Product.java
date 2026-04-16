package com.clone.getchu.domain.product.entity;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProductEnum status;

    @Column(name = "like_count")
    @ColumnDefault("0")
    private Integer likeCount = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // --- Builder 패턴 ---
    @Builder
    public Product(Member seller, Category category, String title, String description,
                   Integer price, ProductEnum status, Integer likeCount, Boolean isDeleted) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status;
        this.likeCount = (likeCount != null) ? likeCount : 0;
        this.isDeleted = (isDeleted != null) ? isDeleted : false;
    }

    // --- 비즈니스 로직 ---
    public void updateProduct(String title, String description, Integer price, ProductEnum status, Category category, List<String> imageUrls) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (status != null) this.status = status;
        if (category != null) this.category = category;
        updateImages(imageUrls);
    }

    public void updateStatus(ProductEnum status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void updateImages(List<String> newUrls) {
        // 1. null이면 "수정 의사가 없음"으로 판단하여 기존 상태 유지
        if (newUrls == null) {
            return;
        }

        // 2. null이 아니면 일단 기존 이미지를 모두 비움
        // (빈 리스트([])가 들어오면 아래 루프를 타지 않으므로 '전체 삭제'가 됨)
        List<String> distinctUrls = newUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .toList();
        this.images.clear();

        // 3. 새 URL이 있다면 추가
        distinctUrls.forEach(url -> {
            ProductImage productImage = new ProductImage(url, this);
            this.images.add(productImage);
        });
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

}
