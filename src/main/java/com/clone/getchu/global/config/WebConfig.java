package com.clone.getchu.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS 설정은 SecurityConfig.corsConfigurationSource() 에서 단일 관리합니다.
 * Spring Security 필터 체인이 활성화된 경우 WebMvcConfigurer.addCorsMappings()는
 * 인증이 필요한 요청에 적용되지 않으므로 SecurityConfig의 설정이 권위적입니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setFallbackPageable(PageRequest.of(0, 20)); // 기본값
        resolver.setMaxPageSize(100); // [핵심] 최대 페이지 사이즈 100개로 강제 제한
        resolvers.add(resolver);
    }
}
