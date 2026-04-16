package com.clone.getchu.domain.product.repository;

import com.clone.getchu.domain.product.dto.ProductSearchCondition;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
                        combineCursorCondition(condition.cursor()),
                        eqCategoryId(condition.categoryId()),
                        containsKeyword(condition.keyword()),
                        eqStatus(condition.status()),
                        product.isDeleted.isFalse()
                )
                .orderBy(product.createdAt.desc(), product.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return convertToCursorPage(pageable, content);
    }

    private BooleanExpression combineCursorCondition(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;

        try {
            // 1. 형식 검증 및 분리
            String[] parts = cursor.split("_");
            if (parts.length != 2) {
                throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
            }

            // 2. 데이터 파싱
            LocalDateTime cursorDateTime = LocalDateTime.parse(parts[0]);
            Long cursorId = Long.valueOf(parts[1]);

            // 3. 조건 생성
            return product.createdAt.lt(cursorDateTime)
                    .or(product.createdAt.eq(cursorDateTime).and(product.id.lt(cursorId)));

        } catch (DateTimeParseException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR_FORMAT);
        }
    }
    // --- 동적 쿼리용 BooleanExpression ---

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
