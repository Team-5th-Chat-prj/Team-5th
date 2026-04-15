package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.domain.trade.dto.response.GetAllTradeResponse;
import com.clone.getchu.domain.trade.dto.response.GetTradeDetailResponse;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    //private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    /**
     * 상품 예약 요청 (SALE → RESERVED)
     * 1. 상품 존재 확인
     * 2. 구매자가 판매자 본인이 아닌지 확인
     * 3. 상품 상태 전이(proceed) — 내부적으로 TradeStatus.SALE.next() 호출
     * 4. Trade 생성 저장
     */
//    @Transactional
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

    @Transactional
    public void updateTradeStatus(Long tradeId, TradeStatus targetStatus, Long memberId){
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));

        //거래의 당사자가 맞는지 검증
        trade.validateParticipant(memberId);

        if(targetStatus == TradeStatus.SALE){
            trade.cancel();
        } else {
            // "진행" 흐름: SALE -> RESERVED -> TRADING -> SOLD 순차 진행
            validateTransition(trade.getStatus(), targetStatus);
            trade.proceed();
        }

        //product 상태 동기화

    }
    //상태 전이 검증
    private void validateTransition(TradeStatus currentStatus, TradeStatus targetStatus){
        if (currentStatus.next() != targetStatus){
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    //거래 상세 조회
    @Transactional(readOnly = true)
    public GetTradeDetailResponse getTradeDetail(Long tradeId, Long memberId) {
        Trade trade = tradeRepository.findWithAllById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));

        //거래 참여자 검증
        trade.validateParticipant(memberId);
        
        return GetTradeDetailResponse.from(trade);
    }

    //거래 목록 조회
    @Transactional(readOnly = true)
    public List<GetAllTradeResponse> getMyTrade(Long memberId, String role){
        List<Trade> trades;

        if("selling".equals(role)){
            trades = tradeRepository.findAllBySellerIdOrderByCreatedAtDesc(memberId);
        } else if ("buying".equals(role)) {
            trades = tradeRepository.findAllByBuyerIdOrderByCreatedAtDesc(memberId);
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return trades.stream()
                .map(GetAllTradeResponse::from)
                .toList();
    }
}

