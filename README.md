# 겟츄 (Getchu) — 중고거래 플랫폼 백엔드

> 위치 인증 기반의 지역 중고거래 플랫폼 백엔드 시스템  
> JWT 인증, 실시간 채팅, 거래 상태 관리, 리뷰/평점 반영, Redis 기반 동시성 제어와 캐싱을 직접 구현한 학습 목적 프로젝트

---

## 📑 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [팀 구성](#팀-구성)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [패키지 구조](#패키지-구조)
- [시작하기 (로컬 실행)](#시작하기-로컬-실행)
- [환경 변수](#환경-변수)
- [API 문서](#api-문서)
- [SA 문서](#sa-문서)
- [컨벤션](#컨벤션)

---

## 프로젝트 소개

**겟츄(Getchu)** 는 경력 3개월 백엔드 개발자 4명이 3주 동안 구축한 중고거래 플랫폼 백엔드입니다.  
단순 CRUD를 넘어 실무에서 요구되는 다음 기술들을 직접 적용하는 것이 핵심 목표입니다.

| 문제 | 적용 기술 |
|---|---|
| 동시에 여러 명이 같은 상품 구매 시 데이터 정합성 | Redis Lettuce 분산락 (SETNX 직접 구현) |
| 구매자-판매자 실시간 채팅 | WebSocket + STOMP |
| 내 동네 기반 반경 내 상품 조회 | 카카오 지도 API + MySQL Spatial (GEOMETRY) |

---

## 기술 스택

| 계층 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| ORM | Spring Data JPA + QueryDSL 5 |
| DB | MySQL 8.4 |
| Spatial | Hibernate Spatial + JTS |
| Cache (로컬) | Caffeine (인메모리, 인기 검색어) |
| Cache (분산) | Redis 7.4 + Lettuce (Cache-Aside) |
| 실시간 통신 | WebSocket + STOMP |
| 외부 API | 카카오 지도 API (Geocoding / Reverse Geocoding) |
| 인증 | Spring Security + JWT (jjwt 0.12) |
| 문서화 | Spring REST Docs + restdocs-api-spec (OpenAPI 3 YAML 자동 생성) |
| 컨테이너 | Docker / Docker Compose |
| CI | GitHub Actions (PR 시 AI 코드 리뷰 자동 실행) |

---

## 팀 구성

| 이름 | 담당 영역 |
|---|---|
| 신현민 | 프로젝트 세팅, 회원·인증인가(JWT/Security), 리뷰, 카카오맵 API(동네 인증·근처 상품), Docker, 프론트엔드, REST Docs·E2E |
| 김인목 | 채팅(WebSocket/STOMP), Redis 캐싱 |
| 정채림 | 상품 CRUD, 카테고리, 상품 검색(QueryDSL), 상품 상세 Redis Cache-Aside |
| 소수경 | 거래 상태 관리, 동시성 제어(Lettuce 분산락), 찜하기 |

---

## 주요 기능

- **회원**: 회원가입 / 카카오 소셜 로그인 / JWT 인증(Access + Refresh) / 동네 인증
- **상품**: 등록·수정·삭제·조회 / 키워드+카테고리 동적 검색 (QueryDSL)
- **거래**: 판매중 → 예약중 → 거래중 → 판매완료 → 리뷰 상태 흐름 관리
- **동시성 제어**: 같은 상품에 대해 1명만 예약 가능 (Redis Lettuce SETNX 분산락)
- **채팅**: 1:1 실시간 채팅 (WebSocket/STOMP), 커서 기반 페이지네이션, 읽음 처리
- **찜하기**: 관심 상품 등록·해제
- **위치 기반**: 카카오 API를 이용한 동네 인증, 인증된 동네 기준 반경 내 상품 조회 (거리 오름차순)
- **리뷰**: 거래 완료 후 거래 상대방에 대한 리뷰 작성

---

## 아키텍처

```
[클라이언트 (브라우저)]
    ↓ HTTP / WS
[Spring Boot App (Docker)]
    ├── MySQL 8.4
    ├── Redis 7.4  ← 분산락 + Cache-Aside
    └── 카카오 지도 API (외부)
```

> 로컬 실행 환경 기준입니다. 인프라 설계 문서는 [08-인프라 아키텍처 다이어그램](docs/SA/08-인프라%20아키텍처%20다이어그램.md)을 참고하세요.

---

## 패키지 구조

```
src/main/java/com/clone/getchu/
├── GetchuApplication.java
├── domain/
│   ├── auth/          # JWT 인증·인가, 카카오 소셜 로그인
│   ├── member/        # 회원 CRUD, 동네 인증
│   ├── product/       # 상품 등록·수정·삭제·조회, 검색
│   ├── category/      # 카테고리 관리
│   ├── trade/         # 거래 상태 관리
│   ├── chat/          # 1:1 채팅 (WebSocket/STOMP), 읽음 처리
│   ├── like/          # 찜하기
│   ├── review/        # 거래 후 리뷰
│   ├── search/        # 인기 검색어 캐싱
│   └── location/      # 위치 기반 서비스 (카카오 지도 API)
└── global/
    ├── client/        # 외부 API 클라이언트 (카카오 지도 API)
    ├── common/        # 공통 응답 래퍼 (ApiResponse), BaseEntity, CursorPageResponse
    ├── config/        # Security, Redis, WebSocket, Querydsl, Cache 설정
    ├── exception/     # 글로벌 예외 처리, ErrorCode
    ├── lock/          # Redis 분산락 (LockService, RedisLockRepository)
    ├── security/      # JWT 필터, JwtProvider, CustomUserDetails, STOMP 인터셉터
    ├── util/          # 공통 유틸리티
    └── validation/    # 커스텀 유효성 검증
```

---

## 시작하기 (로컬 실행)

### 사전 요구사항

- Docker Desktop 설치 및 실행 중
- Git

### 실행 방법

```bash
# 1. 레포지토리 클론
git clone https://github.com/Team-5th-Chat-prj/Team-5th.git
cd Team-5th

# 2. 환경 변수 파일 생성
cp .env.example .env
# .env 파일을 열어 각 항목 채우기 (아래 환경 변수 섹션 참고)

# 3-1. JAR 빌드
./gradlew bootJar

# 3-2. Docker Compose 실행 (또는 스크립트 한 방에)
./scripts/compose-up.sh
```

> **팀 룰**: IDE(IntelliJ)에서 직접 실행하지 않고 Docker Compose로만 실행합니다.  
> DB_URL은 `jdbc:mysql://mysql:3306/getchu` (컨테이너 내부 통신)으로 고정입니다.

### 프론트엔드와 연동

프론트엔드와 연결해서 실행하려면 → [프론트엔드 레포](https://github.com/Team-5th-Chat-prj/get-chu-frontend)를 참고하세요.

### 개발 중 데이터베이스 접속

| 항목 | 값 |
|---|---|
| Host | `localhost` |
| Port | `3307` (외부 포트, `.env`의 `MYSQL_EXTERNAL_PORT`) |
| Database | `getchu` |
| Username | `root` |
| Password | `.env`의 `DB_PASSWORD` 값 |

### Redis 접속

```bash
docker exec -it getchu-redis redis-cli ping
# → PONG
```

---

## 환경 변수

`.env.example`을 복사해서 `.env`를 만든 뒤 값을 채워주세요.  
`.env`는 `.gitignore`에 등록되어 있어 Git에 커밋되지 않습니다.

| 변수 | 설명 | 예시 |
|---|---|---|
| `APP_EXTERNAL_PORT` | 앱 외부 접근 포트 | `8080` |
| `DB_URL` | MySQL JDBC URL (Docker 실행 시 고정) | `jdbc:mysql://mysql:3306/getchu` |
| `DB_USERNAME` | DB 접속 계정 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | _(직접 설정)_ |
| `MYSQL_DATABASE` | MySQL 생성 DB명 | `getchu` |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 (`DB_PASSWORD`와 동일) | _(직접 설정)_ |
| `MYSQL_EXTERNAL_PORT` | MySQL 외부 포트 (IntelliJ DB 접속용) | `3307` |
| `REDIS_HOST` | Redis 호스트 (Docker 실행 시 고정) | `redis` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `REDIS_EXTERNAL_PORT` | Redis 외부 포트 | `6379` |
| `JWT_SECRET_KEY` | JWT 서명 키 (32자 이상 권장) | _(직접 설정)_ |
| `JWT_ACCESS_EXPIRATION_TIME` | Access Token 만료 시간 (ms) | `900000` (15분) |
| `JWT_REFRESH_EXPIRATION_TIME` | Refresh Token 만료 시간 (ms) | `604800000` (7일) |
| `KAKAO_API_KEY` | 카카오 REST API 키 | _(팀장에게 공유 받기)_ |

---

## API 문서

### OpenAPI YAML 자동 생성

이 프로젝트는 **Spring REST Docs + restdocs-api-spec** 을 사용합니다.  
테스트 코드를 통과해야만 API 명세가 생성되므로, 코드와 문서가 항상 동기화됩니다.

```bash
# 테스트 실행 → 스니펫 생성 → OpenAPI YAML 생성 → docs/api-spec 복사까지 한 번에
./gradlew copyOpenApi

# 생성 위치
docs/api-spec/openapi3.yaml
```

> 테스트가 하나라도 실패하면 YAML이 생성되지 않습니다.

### REST Docs 테스트 작성 가이드

새 API 테스트를 작성할 때는 [docs/rest-docs-guide.md](docs/rest-docs-guide.md)를 참고하세요.

### WebSocket 채팅 테스트

REST API는 OpenAPI YAML로 테스트할 수 있으나, WebSocket/STOMP는 별도 클라이언트가 필요합니다.

**Postman 사용 (권장)**

1. Postman에서 New → WebSocket 선택
2. URL: `ws://localhost:8080/ws-chat/websocket`
3. Headers에 `Authorization: Bearer {JWT 토큰}` 추가 후 Connect
4. STOMP 프레임 형식은 [docs/SA/06-API 명세서.md](docs/SA/06-API%20명세서.md) 의 WebSocket/STOMP 명세 참고

---

## SA 문서

설계 문서 전체는 [`docs/SA/`](docs/SA/) 폴더에 있습니다.

| 번호 | 문서명 |
|---|---|
| 01 | [프로젝트 개요서](docs/SA/01-프로젝트%20개요서.md) |
| 02 | [사용자 시나리오](docs/SA/02-사용자시나리오.md) |
| 03 | [유스케이스 명세서](docs/SA/03-유스케이스%20명세서.md) |
| 04 | [기능 명세서](docs/SA/04-기능%20명세서.md) |
| 05 | [ERD](docs/SA/05-ERD.md) |
| 06 | [API 명세서](docs/SA/06-API%20명세서.md) |
| 07 | [화면 설계서](docs/SA/07-화면%20설계서.md) |
| 08 | [인프라 아키텍처 다이어그램](docs/SA/08-인프라%20아키텍처%20다이어그램.md) |
| 09 | [동시성 제어 설계서](docs/SA/09-동시성%20제어%20설계서.md) |
| 10 | [ADR (Architecture Decision Record)](docs/SA/10-ADR%20(Architecture%20Decision%20Record).md) |
| 11 | [패키지 구조 설계서](docs/SA/11-패키지%20구조%20설계서.md) |
| 12 | [에러코드 설계서](docs/SA/12-에러코드%20설계서.md) |

---

## 컨벤션

### 브랜치 전략

```
main        ← 메인 브랜치
develop     ← 통합 브랜치
feat/{기능명} ← 기능 개발 브랜치
fix/{이슈번호} ← 버그 수정 브랜치
```

### PR / 이슈

- PR 작성 시 `.github/PULL_REQUEST_TEMPLATE.md` 양식을 따릅니다.
- 이슈 작성 시 `.github/ISSUE_TEMPLATE/` 내 템플릿을 사용합니다. (`bugfix`, `feature`, `refactor`)

### GitHub Actions

| 워크플로우 | 트리거 | 내용 |
|---|---|---|
| `ai-review.yml` | PR 생성/수정 | PR diff를 외부 AI 리뷰 서버로 전송하여 코드 리뷰 코멘트 자동 작성 |
