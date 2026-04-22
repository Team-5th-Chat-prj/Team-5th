package com.clone.getchu.support;

import com.clone.getchu.global.config.SecurityConfig;
import com.clone.getchu.global.security.JwtAccessDeniedHandler;
import com.clone.getchu.global.security.JwtAuthEntryPoint;
import com.clone.getchu.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// @WebMvcTest와 @AutoConfigureMockMvc는 각 테스트 클래스에서 선언합니다.
// 기반 클래스에 @WebMvcTest를 두면 컨트롤러를 특정하지 않아 모든 @Controller를 로드하려 시도합니다.
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;

    // ── 공통 인프라 MockBean ─────────────────────────────────────────────────
    // SecurityConfig + JwtAuthFilter가 의존하는 빈들을 한 곳에서 관리
    // 각 테스트 클래스에서 반복 선언하지 않아도 됨

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;  // RedisConfig 의존성 해소
    @MockBean
    private StringRedisTemplate stringRedisTemplate;        // JwtAuthFilter 블랙리스트 체크

    @MockBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;
    @MockBean
    private JwtProvider jwtProvider;                        // resolveToken() → null 반환 → 필터 통과

    @BeforeEach
    void setUp(WebApplicationContext context,
               RestDocumentationContextProvider restDocumentation) {
        // 미인증 요청 시 JwtAuthEntryPoint 목이 실제로 401을 내려주도록 설정
        // 기본 Mockito 목은 void 메서드를 no-op으로 처리해서 응답 상태가 바뀌지 않음
        // commence() 자체가 throws IOException을 선언하므로 호출 라인을 try-catch로 감쌈
        try {
            willAnswer(invocation -> {
                HttpServletResponse response = invocation.getArgument(1);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }).given(jwtAuthEntryPoint).commence(any(), any(), any());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    // 테스트 간 SecurityContext 격리 — @WithMockCustomUser가 설정한 인증 정보가 다음 테스트로 누출되지 않도록 보장
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
