package com.clone.getchu.domain.search.controller;

import com.clone.getchu.domain.search.dto.PopularKeywordResponse;
import com.clone.getchu.domain.search.service.SearchService;
import com.clone.getchu.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularKeywordResponse>>> getPopularKeywords() {
        return ResponseEntity.ok(ApiResponse.success(searchService.getPopularKeywords()));
    }
}