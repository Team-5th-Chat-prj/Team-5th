package com.clone.getchu.domain.trade.service;

import com.clone.getchu.domain.member.entity.Member;
import com.clone.getchu.domain.product.entity.Product;
import com.clone.getchu.domain.trade.entity.Trade;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @InjectMocks
    private TradeService tradeService;

    @Mock
    private TradeRepository tradeRepository;

    @Test
    @DisplayName("타겟 상태가 RESERVE이면 trade.cancel()이 호출되고 상태가 SALE로 변경된다")
    void updateStatus_Cancel() {
        //Given
        Trade realTrade = Trade.builder()
                .product(mock(Product.class))
                .buyer(mock(Member.class))
                .seller(mock(Member.class))
                .status(TradeStatus.RESERVED)
                .build();

        Trade spyTrade = spy(realTrade);

        given(tradeRepository.findById(anyLong())).willReturn(Optional.of(spyTrade));
        doNothing().when(spyTrade).validateParticipant(anyLong());

        //When
        tradeService.updateTradeStatus(1L, TradeStatus.SALE, 1L);

        //Then
        verify(spyTrade).cancel();
        assertThat(spyTrade.getStatus()).isEqualTo(TradeStatus.SALE);
    }

}