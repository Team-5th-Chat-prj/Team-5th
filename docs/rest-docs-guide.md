# REST Docs 테스트 코드 작성 가이드

Spring REST Docs + restdocs-api-spec을 사용해 컨트롤러 테스트를 작성하면,  
테스트 통과 시 `build/generated-snippets/` 아래에 스니펫이 생성되고  
`./gradlew openapi3` 실행 시 `build/api-spec/openapi3.yaml` 이 자동으로 만들어집니다.

---

## 1. 테스트 클래스 기본 구조

```java
@WebMvcTest(XxxController.class)          // 테스트할 컨트롤러 지정
class XxxControllerTest extends RestDocsSupport {

    // ── 필수 MockBean (모든 테스트 클래스에 공통으로 필요) ──────────────────
    @MockBean private RedisConnectionFactory redisConnectionFactory;
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @MockBean private RedissonClient redissonClient;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private JwtAuthEntryPoint jwtAuthEntryPoint;
    @MockBean private JwtAccessDeniedHandler jwtAccessDeniedHandler;
    @MockBean private JwtProvider jwtProvider;

    // ── 테스트할 서비스 ────────────────────────────────────────────────────
    @MockBean XxxService xxxService;

    @Autowired ObjectMapper objectMapper;

    // ── JwtAuthFilter 우회 설정 (인증 로직 없이 컨트롤러만 테스트) ────────
    @BeforeEach
    void setUpJwtFilter() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req   = invocation.getArgument(0);
            HttpServletResponse res  = invocation.getArgument(1);
            FilterChain chain        = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }
}
```

> **왜 이렇게 하나요?**  
> `@WebMvcTest`는 Spring Security 필터 체인까지 로드합니다.  
> `JwtAuthFilter`를 `@MockBean`으로 교체하고 `doFilter`만 통과시켜서  
> 실제 JWT 검증 없이 컨트롤러 로직만 테스트합니다.

---

## 2. 인증 처리 방법

### JWT가 필요한 API
`@WithMockCustomUser` 어노테이션을 테스트 메서드에 붙입니다.  
이것이 `SecurityContextHolder`에 `CustomUserDetails`를 주입해줍니다.

```java
@Test
@WithMockCustomUser(memberId = 1L, email = "test@email.com", nickname = "테스트유저")
void someAuthenticatedApi() throws Exception {
    mockMvc.perform(get("/some/api")
            .header("Authorization", "Bearer test-token"))   // 헤더 값은 임의 문자열로 OK
        ...
}
```

| 속성 | 기본값 | 설명 |
|---|---|---|
| `memberId` | `1L` | `userDetails.getMemberId()` 반환값 |
| `email` | `"test@email.com"` | `userDetails.getUsername()` 반환값 |
| `nickname` | `"테스트유저"` | 닉네임 |
| `role` | `"USER"` | 권한 (`ROLE_USER`로 변환됨) |

### 비로그인도 가능한 API
`@WithAnonymousUser`를 붙이고 Authorization 헤더를 생략합니다.

```java
@Test
@WithAnonymousUser
void publicApi() throws Exception {
    mockMvc.perform(get("/public/resource"))
        ...
}
```

---

## 3. 테스트 메서드 작성 패턴

```java
@Test
@DisplayName("API 설명 - HTTP메서드 /경로")
@WithMockCustomUser          // JWT 필요한 경우
void methodName() throws Exception {
    // 1. given: 서비스 목 응답 설정
    given(xxxService.someMethod(any())).willReturn(/* 응답 픽스처 */);

    // 2. when & then: 요청 수행 + 상태 검증 + 문서화
    // ⚠️ requestHeaders()에 Authorization을 선언했다면 실제 요청에도 반드시 헤더를 포함해야 합니다.
    //    REST Docs는 문서화한 헤더가 실제 요청에 존재하는지 검증합니다.
    mockMvc.perform(get("/path")
                    .header("Authorization", "Bearer test-token")  // 실제 값은 임의 문자열 OK — @WithMockCustomUser가 인증 처리
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andDo(MockMvcRestDocumentationWrapper.document(
                    "도메인/api-identifier",          // 스니펫 폴더명 (kebab-case 권장)
                    resource(ResourceSnippetParameters.builder()
                            .tag("Tag명")             // Swagger UI 그룹 이름
                            .summary("API 한 줄 요약")
                            .description("API 상세 설명")
                            .requestHeaders(
                                    headerWithName("Authorization").description("Bearer JWT 액세스 토큰")
                            )
                            // 요청 바디가 있는 경우
                            .requestSchema(Schema.schema("XxxRequest"))
                            .requestFields(
                                    fieldWithPath("fieldName").type(JsonFieldType.STRING).description("필드 설명"),
                                    fieldWithPath("optionalField").type(JsonFieldType.STRING).description("선택 필드").optional()
                            )
                            // 응답 바디
                            .responseSchema(Schema.schema("XxxResponse"))
                            .responseFields(
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("응답 메시지"),
                                    fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("ID")
                            )
                            .build()
                    )
            ));
}
```

---

## 4. HTTP 메서드별 MockMvc 요청 작성

> `RestDocumentationRequestBuilders`를 import해서 사용합니다.  
> (경로 변수 문서화를 위해 `MockMvcRequestBuilders` 대신 이걸 써야 합니다.)

```java
// GET (경로 변수 없음)
mockMvc.perform(get("/members/me")
        .header("Authorization", "Bearer test-token"))

// GET (경로 변수 있음) — pathParameters() 문서화 가능
mockMvc.perform(get("/members/{memberId}", 1L))

// POST / PATCH (요청 바디 있음)
mockMvc.perform(post("/some/path")
        .header("Authorization", "Bearer test-token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestObject)))

// DELETE
mockMvc.perform(delete("/members/me")
        .header("Authorization", "Bearer test-token"))

// GET (쿼리 파라미터)
mockMvc.perform(get("/products")
        .param("page", "1")
        .param("size", "10"))
```

---

## 5. 필드 타입 및 옵션 정리

### JsonFieldType
| 타입 | 사용 예 |
|---|---|
| `STRING` | 문자열, 날짜/시간(ISO 형식), enum 값 |
| `NUMBER` | 정수, 소수 |
| `BOOLEAN` | true / false |
| `OBJECT` | 중첩 객체 |
| `ARRAY` | 배열 |
| `NULL` | 항상 null인 필드 |

### 자주 쓰는 옵션
```java
fieldWithPath("field").type(STRING).description("설명")           // 필수 필드
fieldWithPath("field").type(STRING).description("설명").optional() // nullable / 생략 가능 필드
```

### 중첩 객체 / 배열
```java
// 중첩 객체: data.address.city
fieldWithPath("data.address.city").type(STRING).description("도시")

// 배열: data[].id
fieldWithPath("data[].id").type(NUMBER).description("ID")
fieldWithPath("data[].name").type(STRING).description("이름")
```

### 경로 변수 문서화
```java
.pathParameters(
    parameterWithName("memberId").description("조회할 회원 ID")
)
```

### 쿼리 파라미터 문서화
```java
.queryParameters(
    parameterWithName("page").description("페이지 번호 (기본값: 1)"),
    parameterWithName("size").description("페이지 크기 (기본값: 10)")
)
```

---

## 6. 응답 스키마 이름 명시 (중요)

`.responseSchema()`와 `.requestSchema()`를 **항상** 지정하세요.  
지정하지 않으면 스키마 이름이 `members-me248838607` 같이 경로 기반 해시로 자동 생성되어 yaml이 알아보기 어려워집니다.

```java
ResourceSnippetParameters.builder()
    .requestSchema(Schema.schema("UpdatePasswordRequest"))   // 요청 바디 스키마 이름
    .responseSchema(Schema.schema("UpdatePasswordResponse")) // 응답 바디 스키마 이름
    ...
```

**네이밍 규칙:**
- 요청 바디: `{기능명}Request` (예: `MemberUpdateRequest`, `UpdatePasswordRequest`)
- 응답 바디: `{기능명}Response` (예: `MemberResponse`, `DeleteMemberResponse`)
- void 응답도 이름을 붙여줘야 합니다 (예: `DeleteMemberResponse`, `UpdatePasswordResponse`)

---

## 7. 응답 없는 API (void)

`delete`, `patch` 등에서 data 없이 메시지만 반환하는 경우 `data` 필드는 `@JsonInclude(NON_NULL)` 때문에 응답에 포함되지 않습니다.

```java
.responseFields(
    fieldWithPath("code").type(JsonFieldType.STRING).description("응답 코드 (SUCCESS)"),
    fieldWithPath("message").type(JsonFieldType.STRING).description("처리 결과 메시지")
    // data 필드는 null이므로 responseFields에 작성하지 않음
)
```

---

## 7. 서비스 목(Mock) 설정 패턴

```java
// 값 반환
given(service.findById(anyLong())).willReturn(responseFixture);
given(service.search(any())).willReturn(List.of(item1, item2));

// void 메서드
doNothing().when(service).delete(anyLong());

// 특정 인자 매칭
given(service.findById(1L)).willReturn(responseFixture);
given(service.update(any(CustomUserDetails.class), any(UpdateRequest.class))).willReturn(updated);
```

---

## 8. 전체 예시 (회원 정보 조회 참고)

`MemberControllerTest.java`를 참고하세요.  
구조가 잡혀 있으니 새 도메인 테스트는 복사 후 서비스/DTO만 교체하면 됩니다.

---

## 9. OpenAPI yaml 생성 방법

```bash
# 테스트 실행 → 스니펫 생성 → yaml 생성 → docs 복사까지 한 번에
./gradlew copyOpenApi

# 생성 위치
docs/api-spec/openapi3.yaml
```

테스트가 **하나라도 실패하면 yaml이 생성되지 않습니다.**  
이것이 명세와 코드를 항상 동기화시켜주는 핵심입니다.