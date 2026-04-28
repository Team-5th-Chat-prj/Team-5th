# 빌드 단계
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 1. Gradle 래퍼 및 설정 파일 복사
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

# 2. 의존성 다운로드 (소스 코드 변경되어도 캐시됨)
RUN gradle dependencies --no-daemon || true

# 3. 전체 소스코드 복사 후 빌드
COPY src src
RUN gradle build -x test --no-daemon

# 실행 단계
FROM eclipse-temurin:17-jre
# JRE만 포함 → 실행 단계에서 컴파일러 불필요 → 이미지 크기 감소
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]