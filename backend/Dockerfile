FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar && cp build/libs/app.jar app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S buta && adduser -S -G buta -u 10001 buta
WORKDIR /app
COPY --from=build --chown=buta:buta /workspace/app.jar app.jar
USER 10001
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health/liveness" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
