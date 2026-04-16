package com.clone.getchu.domain.product.service;

import com.clone.getchu.domain.category.entity.Category;
import com.clone.getchu.domain.category.repository.CategoryRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.domain.product.dto.*;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.global.common.CursorPageResponse;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, Long sellerId) {
        Member seller = memberRepository.findById(sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .status(ProductEnum.SALE)
                .build();

        product.updateImages(request.imageUrls());

        productRepository.save(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, Long memberId) {
        Product product = findActiveProduct(productId);

        validateSeller(product, memberId);

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        product.updateProduct(
                request.title(),
                request.description(),
                request.price(),
                request.status(),
                category,
                request.imageUrls()
        );

        return ProductResponse.from(product);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = findActiveProduct(productId);
        return ProductResponse.from(product);
    }

    public CursorPageResponse<ProductListResponse> searchProducts(ProductSearchCondition cond, Pageable pageable) {
        ProductSearchCondition searchCond = applyDefaultStatus(cond);

        CursorPageResponse<Product> productSlice = productRepository.searchByCursor(searchCond, pageable);

        List<ProductListResponse> dtoList = productSlice.getContent().stream()
                .map(ProductListResponse::from)
                .toList();

        return new CursorPageResponse<>(
                dtoList,
                productSlice.getNextCursor(),
                productSlice.isHasNext()
        );
    }

    @Transactional
    public void deleteProduct(Long productId, Long memberId) {
        // [수정] 이미 삭제된 상품을 또 삭제하지 않도록 검증
        Product product = findActiveProduct(productId);

        validateSeller(product, memberId);

        product.softDelete();
    }

    // --- Helper Methods ---

    private Product findActiveProduct(Long productId) {
        return productRepository.findDetailById(productId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateSeller(Product product, Long memberId) {
        if (!product.getSeller().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
    private ProductSearchCondition applyDefaultStatus(ProductSearchCondition cond) {
        if (cond.status() == null) {
            // Record라면 새로운 객체 생성, 일반 클래스라면 setter나 copy 메서드 활용
            return new ProductSearchCondition(
                    cond.keyword(),
                    cond.categoryId(),
                    ProductEnum.SALE,
                    cond.cursor()
            );
        }
        return cond;
    }
}