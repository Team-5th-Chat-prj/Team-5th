package com.clone.getchu.domain.search.dto;

public record PopularKeywordResponse(
        String keyword,
        Long searchCount
) {
}