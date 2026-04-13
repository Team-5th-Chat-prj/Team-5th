# 빌드 단계
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 1. 의존성 관련 파일 먼저 복사 (캐시 활용)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 2. 전체 소스코드 복사 후 빌드
COPY . .
RUN gradle build -x test --no-daemon

# 실행 단계
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]