package com.clone.getchu.domain.search.service;

import com.clone.getchu.domain.search.dto.PopularKeywordResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String RANKING_KEY = "search:ranking";

    /**
     * 검색 키워드 카운팅 (검색 시 호출)
     */
    public void saveKeyword(String keyword) {
        // ZSET에서 해당 키워드의 스코어를 1 증가시킴
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1);
    }

    /**
     * 인기 검색어 TOP 10 조회
     */
    public List<PopularKeywordResponse> getPopularKeywords() {
        // 내림차순으로 상위 10개 조회 (0 ~ 9)
        Set<ZSetOperations.TypedTuple<String>> range =
                redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, 9);

        if (range == null) return List.of();

        return range.stream()
                .map(tuple -> new PopularKeywordResponse(
                        tuple.getValue(),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                ))
                .toList();
    }
}
