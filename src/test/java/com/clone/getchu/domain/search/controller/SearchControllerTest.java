package com.clone.getchu.domain.search.controller;

import com.clone.getchu.domain.search.dto.PopularKeywordResponse;
import com.clone.getchu.domain.search.service.SearchService;
import com.clone.getchu.support.RestDocsSupport;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTest extends RestDocsSupport {

    @MockBean
    private SearchService searchService;

    @Test
    @DisplayName("인기 검색어 TOP 10 조회 - GET /search/popular")
    void getPopularKeywords() throws Exception {
        // given
        List<PopularKeywordResponse> keywords = List.of(
                new PopularKeywordResponse("아이폰", 120L),
                new PopularKeywordResponse("맥북", 95L),
                new PopularKeywordResponse("나이키", 80L)
        );
        given(searchService.getPopularKeywords()).willReturn(keywords);

        // when & then
        mockMvc.perform(get("/search/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].keyword").value("아이폰"))
                .andExpect(jsonPath("$.data[0].searchCount").value(120L))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "search/popular-keywords",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Search")
                                .summary("인기 검색어 TOP 10 조회")
                                .description("최근 검색 빈도 기준 상위 10개 인기 검색어를 반환합니다. 비로그인 상태에서도 조회 가능합니다.")
                                .responseSchema(Schema.schema("PopularKeywordsResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data[].keyword").type(JsonFieldType.STRING).description("검색어"),
                                        fieldWithPath("data[].searchCount").type(JsonFieldType.NUMBER).description("검색 횟수")
                                )
                                .build()
                        )
                ));
    }
}
