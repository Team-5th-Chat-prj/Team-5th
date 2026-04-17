package com.clone.getchu.domain.trade.controller;

import com.clone.getchu.domain.trade.dto.request.TradeStatusUpdateRequest;
import com.clone.getchu.domain.trade.dto.response.TradeReserveResponse;
import com.clone.getchu.domain.trade.enums.TradeStatus;
import com.clone.getchu.domain.trade.service.TradeFacade;
import com.clone.getchu.domain.trade.service.TradeService;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TradeController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.clone\\.getchu\\.global\\.(security|config\\.SecurityConfig).*"
        )
)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 보내는 대역

    @MockBean
    private TradeService tradeService;

    @MockBean
    private TradeFacade tradeFacade;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("상품 예약 요청 시 200 OK와 예약 정보를 반환한다")
    void reserveProduct_Success() throws Exception {
        // given
        Long productId = 1L;
        Long buyerId = 2L;

        TradeReserveResponse response = new TradeReserveResponse(
                100L,
                "아이폰 17",
                "판매자닉네임",
                "구매자닉네임"
        );

        // 서비스 호출 시 결과값 모킹
        // buyerId is expected to arise from userDetails.getMemberId(), which might not be buyerId in WithMockUser default. 
        // We will just use any(Long.class) for memberId since WithMockUser's principal handling might vary.
        given(tradeFacade.reserveProduct(eq(productId), any()))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/products/{productId}/reserve", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())) // 시큐리티 보호 통과
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("SUCCESS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.tradeId").value(100L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.productTitle").value("아이폰 17"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.buyerNickname").value("구매자닉네임"))
                .andDo(print());
    }


    @Test
    @WithMockUser
    @DisplayName("거래 상태 변경 성공 시 200 OK를 반환한다")
    void updateTradeStatus_Success() throws Exception {
        // given
        Long tradeId = 1L;
        TradeStatusUpdateRequest request = new TradeStatusUpdateRequest(TradeStatus.TRADING);

        doNothing().when(tradeService).updateTradeStatus(eq(tradeId), any(), any());

        // when & then
        mockMvc.perform(patch("/trades/{tradeId}/status", tradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "trade/update-status",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Trade")
                                .summary("거래 상태 변경")
                                .pathParameters(parameterWithName("tradeId").description("거래 ID"))
                                .requestFields(fieldWithPath("status").description("변경할 상태 (RESERVED, TRADING, SOLD, SALE)"))
                                .build())
                ));
    }
}