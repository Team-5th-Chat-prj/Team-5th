package com.clone.getchu.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String POPULAR_KEYWORDS = "popularKeywords";
    private static final int POPULAR_KEYWORDS_TTL_MINUTES = 10;
    // 검색어 순위는 전체 목록을 하나의 값으로 캐싱하므로 최대 1개 엔트리로 충분
    private static final int POPULAR_KEYWORDS_MAX_SIZE = 1;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(POPULAR_KEYWORDS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(POPULAR_KEYWORDS_TTL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(POPULAR_KEYWORDS_MAX_SIZE));
        return cacheManager;
    }
}
