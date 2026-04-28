package com.clone.getchu.domain.category.controller;

import com.clone.getchu.domain.category.dto.CategoryResponse;
import com.clone.getchu.domain.category.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest extends RestDocsSupport {

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록 조회 - GET /categories")
    void getAllCategories() throws Exception {
        // given
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1L, "디지털기기"),
                new CategoryResponse(2L, "생활가전"),
                new CategoryResponse(3L, "패션")
        );
        given(categoryService.getCategories()).willReturn(categories);

        // when & then
        mockMvc.perform(get("/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("디지털기기"))
                .andDo(MockMvcRestDocumentationWrapper.document(
                        "category/get-categories",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Category")
                                .summary("카테고리 목록 조회")
                                .description("등록된 모든 카테고리 목록을 조회합니다. 비로그인 상태에서도 조회 가능합니다.")
                                .responseSchema(Schema.schema("CategoryListResponse"))
                                .responseFields(
                                        fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지").optional(),
                                        fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("카테고리 ID"),
                                        fieldWithPath("data[].name").type(JsonFieldType.STRING).description("카테고리명")
                                )
                                .build()
                        )
                ));
    }
}
