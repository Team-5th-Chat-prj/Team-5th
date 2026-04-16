package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.dto.ProductSearchCondition;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.global.common.CursorPageResponse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static com.clone.getchu.domain.product.entity.QProduct.product;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorPageResponse<Product> searchByCursor(ProductSearchCondition condition, Pageable pageable) {
        List<Product> content = queryFactory
                .selectFrom(product)
                .leftJoin(product.category).fetchJoin() // 페치 조인으로 성능 최적화
                .where(
                        ltCursorId(condition.cursor()), // Cursor 조건
                        eqCategoryId(condition.categoryId()),
                        containsKeyword(condition.keyword()),
                        eqStatus(condition.status()),
                        product.isDeleted.isFalse()
                )
                .orderBy(product.id.desc()) // 최신순 정렬
                .limit(pageable.getPageSize() + 1) // 다음 페이지 확인을 위해 +1
                .fetch();

        return convertToCursorPage(pageable, content);
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

        // 2. 다음 페이지 존재 여부 확인 및 초과분 제거
        if (content.size() > pageable.getPageSize()) {
            content.remove(pageable.getPageSize());
            hasNext = true;
        }

        // 3. 다음 커서 값 결정 (마지막 아이템의 ID)
        String nextCursor = null;
        if (!content.isEmpty()) {
            // 현재 리스트의 마지막 항목 ID를 다음 요청의 커서로 사용
            Product lastProduct = content.get(content.size() - 1);
            nextCursor = String.valueOf(lastProduct.getId());
        }

        // 4. 최종적으로 CursorPageResponse로 감싸서 반환
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
