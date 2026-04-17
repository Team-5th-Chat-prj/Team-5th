package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.product.entity.ProductEnum;
import com.clone.getchu.domain.product.repository.ProductRepository;
import com.clone.getchu.domain.trade.dto.response.GetAllTradeResponse;
import com.clone.getchu.domain.trade.dto.response.GetTradeDetailResponse;
import com.clone.getchu.domain.trade.dto.response.TradeReserveResponse;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeRole;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.member.repository.MemberRepository;
import com.clone.getchu.global.exception.BusinessException;
import com.clone.getchu.global.exception.ErrorCode;
import com.clone.getchu.global.exception.ForbiddenException;
import com.clone.getchu.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    /**
     * 상품 예약 요청 (SALE → RESERVED)
     * 1. 상품 존재 확인
     * 2. 구매자가 상품 판매자 본인이 아닌지 확인
     * 3. 상품 상태 변경
     * 4. Trade 생성 저장
     */
    @Transactional
    public TradeReserveResponse reserveProduct(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        //본인 상품 예약 방지 검증
        if (product.getSeller().getId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        //상품이 판매중인지 검증
        if (product.getStatus() != ProductEnum.SALE) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED);
        }

        Member buyer = memberRepository.findById(buyerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        //상품 등록자 추출 (판매자)
        Member seller = product.getSeller();

        //상품 상태 변경
        product.updateStatus(ProductEnum.RESERVED);

        Trade trade = Trade.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .status(TradeStatus.RESERVED)
                .build();

        Trade savedTrade = tradeRepository.save(trade);

        return TradeReserveResponse.from(savedTrade);
    }

    @Transactional
    public void updateTradeStatus(Long tradeId, TradeStatus targetStatus, Long memberId){
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRADE_NOT_FOUND));

        //거래의 당사자가 맞는지 검증
        trade.validateParticipant(memberId);

        TradeRole role = trade.isSeller(memberId) ? TradeRole.SELLER : TradeRole.BUYER;

        if(targetStatus == TradeStatus.SALE){
            trade.cancel(role);
            trade.getProduct().updateStatus(ProductEnum.SALE);
        } else {
            // "진행" 흐름: SALE -> RESERVED -> TRADING -> SOLD 순차 진행
            validateTransition(trade.getStatus(), targetStatus, role);
            trade.proceed(role);
        }

    }
    //상태 전이 검증
    private void validateTransition(TradeStatus currentStatus, TradeStatus targetStatus, TradeRole role){
        if (currentStatus.next(role) != targetStatus){
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
    public List<GetAllTradeResponse> getMyTrade(Long memberId, TradeRole role){
        List<Trade> trades;

        if(role == TradeRole.SELLER){
            trades = tradeRepository.findAllBySellerIdOrderByCreatedAtDesc(memberId);
        } else if (role == TradeRole.BUYER) {
            trades = tradeRepository.findAllByBuyerIdOrderByCreatedAtDesc(memberId);
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return trades.stream()
                .map(GetAllTradeResponse::from)
                .toList();
    }
}

