# 1단계: 빌드 이미지
FROM gradle:8.12.1-jdk21 AS builder
WORKDIR /app

# 프로젝트 의존성 캐싱을 위해 먼저 복사
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN gradle --no-daemon build || return 0

# 실제 코드 복사
COPY . .

# Spring Boot 애플리케이션 빌드
RUN gradle clean bootJar --no-daemon

# 2단계: 실행 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 프로덕션 프로파일 사용 (yml에서는 환경변수 기반 설정)
ENV SPRING_PROFILES_ACTIVE=prod

# 포트 노출
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
