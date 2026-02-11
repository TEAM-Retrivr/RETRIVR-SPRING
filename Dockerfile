# ============================
# 1) Build Stage (cached deps)
# ============================
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app

# 1) Gradle/빌드 설정 파일 먼저 복사 (의존성 캐시 레이어)
COPY gradlew ./
COPY gradle/ ./gradle/
COPY build.gradle* settings.gradle* gradle.properties* ./

# gradlew 사용 시 권한 (gradle 이미지라 보통 필요 없지만 안전하게)
RUN chmod +x gradlew

# 2) 의존성만 미리 받아두기 (소스 변경과 무관하게 캐시됨)
# -x test: 보통 컨테이너 빌드에서 테스트는 빼는 편 (원하면 제거)
RUN ./gradlew --no-daemon dependencies || true

# 3) 그 다음에 소스 복사 (여기부터는 소스 변경 시에만 캐시 깨짐)
COPY src/ ./src/

RUN ./gradlew clean bootJar --no-daemon -x test


# ============================
# 2) Runtime Stage
# ============================
FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]