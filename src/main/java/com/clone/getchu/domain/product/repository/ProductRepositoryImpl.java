package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.dto.ProductSearchCondition;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.global.common.CursorPageResponse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static com.clone.getchu.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse<Product> searchByCursor(ProductSearchCondition condition, Pageable pageable) {
        List<Product> content = queryFactory
                .selectFrom(product)
                .leftJoin(product.category).fetchJoin()
                .where(
                        combineCursorCondition(condition.cursor()), // 복합 커서 조건 적용
                        eqCategoryId(condition.categoryId()),
                        containsKeyword(condition.keyword()),
                        eqStatus(condition.status()),
                        product.isDeleted.isFalse()
                )
                .orderBy(product.createdAt.desc(), product.id.desc()) // 복합 정렬
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return convertToCursorPage(pageable, content);
    }

    private BooleanExpression combineCursorCondition(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;

        // 커서 파싱 (예: "2024-04-16T12:00:00_153")
        String[] parts = cursor.split("_");
        LocalDateTime cursorDateTime = LocalDateTime.parse(parts[0]);
        Long cursorId = Long.valueOf(parts[1]);

        return product.createdAt.lt(cursorDateTime)
                .or(product.createdAt.eq(cursorDateTime).and(product.id.lt(cursorId)));
    }
    // --- 동적 쿼리용 BooleanExpression ---

    private BooleanExpression ltCursorId(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        return product.id.lt(Long.valueOf(cursor));
    }

    private BooleanExpression containsKeyword(String keyword) {
        return (keyword != null && !keyword.isBlank()) ? product.title.contains(keyword) : null;
    }

    private BooleanExpression eqCategoryId(Long categoryId) {
        return categoryId != null ? product.category.id.eq(categoryId) : null;
    }

    private BooleanExpression eqStatus(ProductEnum status) {
        return status != null ? product.status.eq(status) : null;
    }

    // 무한 스크롤(Slice) 처리를 위한 유틸 메서드
    private CursorPageResponse<Product> convertToCursorPage(Pageable pageable, List<Product> content) {
        boolean hasNext = false;
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        String nextCursor = null;
        if (!content.isEmpty()) {
            Product lastProduct = content.get(content.size() - 1);
            // 다음 요청을 위한 복합 커서 생성 (createdAt_id)
            nextCursor = String.format("%s_%d", lastProduct.getCreatedAt(), lastProduct.getId());
        }

        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
