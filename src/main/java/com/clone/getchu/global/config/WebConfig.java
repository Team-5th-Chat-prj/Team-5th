package com.clone.getchu.global.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS 설정은 SecurityConfig.corsConfigurationSource() 에서 단일 관리합니다.
 * Spring Security 필터 체인이 활성화된 경우 WebMvcConfigurer.addCorsMappings()는
 * 인증이 필요한 요청에 적용되지 않으므로 SecurityConfig의 설정이 권위적입니다.
 */
@Configuration
public class WebConfig {
}
