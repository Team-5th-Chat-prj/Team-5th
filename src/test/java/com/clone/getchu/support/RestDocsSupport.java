package com.clone.getchu.support;

import com.clone.getchu.global.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

// @WebMvcTest와 @AutoConfigureMockMvc는 각 테스트 클래스에서 선언합니다.
// 기반 클래스에 @WebMvcTest를 두면 컨트롤러를 특정하지 않아 모든 @Controller를 로드하려 시도합니다.
@ExtendWith(RestDocumentationExtension.class)
@Import(SecurityConfig.class)
public abstract class RestDocsSupport {

    protected MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext context,
               RestDocumentationContextProvider restDocumentation) {
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