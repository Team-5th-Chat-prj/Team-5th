package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.domain.trade.dto.response.GetTradeDetailResponse;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    /**
     * 상품 예약 요청 (SALE → RESERVED)
     * 1. 상품 존재 확인
     * 2. 구매자가 판매자 본인이 아닌지 확인
     * 3. 상품 상태 전이(proceed) — 내부적으로 TradeStatus.SALE.next() 호출
     * 4. Trade 생성 저장
     */
//    public void reserveProduct(Long productId, Long buyerId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
//
//        if (product.getSeller().getId().equals(buyerId)) {
//            throw new NotFoundException(ErrorCode.MEMBER_NOT_FOUND);
//        }
//
//        Member member = memberRepository.findById(buyerId)
//                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
//
//        // SALE → RESERVED: Product 상태 전이 (Enum에 위임)
//        product.proceed();
//
//        Trade trade = Trade.builder()
//                .product(product)
//                .member(member)
//                .status(TradeStatus.RESERVED)
//                .build();
//
//        tradeRepository.save(trade);
//    }

    /**
     * 거래 진행 (RESERVED → TRADING)
     * Trade와 Product 상태를 동시에 다음 단계로 이동합니다.
     */
    public void proceedTrade(Long tradeId) {
        Trade trade = findTradeById(tradeId);

        trade.proceed();
        //trade.getProduct().proceed();
    }

    /**
     * 거래 취소 (RESERVED | TRADING → SALE)
     * Trade와 Product 상태를 SALE로 돌립니다.
     */
    public void cancelTrade(Long tradeId) {
        Trade trade = findTradeById(tradeId);

        trade.cancel();
        //trade.getProduct().cancel();
    }

    /**
     * 거래 완료 (TRADING → SOLD)
     * Trade와 Product 상태를 SOLD로 변경합니다.
     */
    public void completeTrade(Long tradeId) {
        Trade trade = findTradeById(tradeId);

        // TRADING → SOLD: Enum에 전이 위임
        trade.proceed();
        //trade.getProduct().proceed();
    }

    //거래 상세 조회
    @Transactional(readOnly = true)
    public GetTradeDetailResponse getTradeDetail(Long tradeId, Long memberId) {
        Trade trade = findTradeById(tradeId);

        //거래 참여자 검증
        trade.validateParticipant(memberId);
        
        return GetTradeDetailResponse.from(trade);
    }

    private Trade findTradeById(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));
    }
}

