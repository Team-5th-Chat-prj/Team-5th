package com.clone.getchu.domain.product.entity;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.member.entity.Member;
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

@Entity
@Table(name = "PRODUCT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product {

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
    private String status;

    @Column(name = "like_count")
    @ColumnDefault("0")
    private Integer likeCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // --- Builder 패턴 ---
    @Builder
    public Product(Member seller, Category category, String title, String description,
                   Integer price, String status, Integer likeCount, Boolean isDeleted) {
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
    public void updateProduct(String title, String description, Integer price, String status, Category category) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.status = status;
        this.category = category;
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
