package com.clone.getchu.domain.trade.repository;

import com.clone.getchu.domain.trade.entity.Trade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    //상세 페이지 조회
    @EntityGraph(attributePaths = {"product", "product.seller", "buyer", "seller"})
    Optional<Trade> findWithAllById(Long id);

    // 나의 판매 내역 조회
    @EntityGraph(attributePaths = {"product", "buyer", "seller"})
    List<Trade> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

    // 나의 구매 내역 조회
    @EntityGraph(attributePaths = {"product", "buyer", "seller"})
    List<Trade> findAllByBuyerIdOrderByCreatedAtDesc(Long buyerId);

}
