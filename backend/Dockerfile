FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace
COPY backend/gradlew backend/gradlew.bat ./backend/
COPY backend/gradle ./backend/gradle
COPY backend/gradle.properties backend/settings.gradle.kts backend/build.gradle.kts ./backend/
COPY backend/src ./backend/src
COPY openapi ./openapi

WORKDIR /workspace/backend
RUN chmod +x gradlew \
    && ./gradlew --no-daemon openApiValidate validateAngularOpenApiOutput bootJar \
    && cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" /workspace/app.jar

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S spring && adduser -S -G spring -u 10001 spring
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar
COPY backend/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod 0555 /usr/local/bin/docker-entrypoint.sh

USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
