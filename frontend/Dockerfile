FROM eclipse-temurin:25-jdk-alpine AS api-client

WORKDIR /workspace
COPY backend/gradlew backend/gradlew.bat ./backend/
COPY backend/gradle ./backend/gradle
COPY backend/gradle.properties backend/settings.gradle.kts backend/build.gradle.kts ./backend/
COPY openapi ./openapi
RUN mkdir -p frontend/build/generated/openapi/typescript-angular \
    && chmod +x backend/gradlew \
    && cd backend \
    && ./gradlew --no-daemon validateAngularOpenApiOutput

FROM node:22.23.4-alpine AS build

WORKDIR /workspace/frontend
RUN npm install --global npm@10.9.9
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
COPY --from=api-client /workspace/frontend/build/generated/openapi/typescript-angular ./build/generated/openapi/typescript-angular
ENV SKIP_API_GENERATION=true
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --chmod=0555 frontend/nginx/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
COPY --chown=101:101 --from=build /workspace/frontend/dist/frontend/browser /usr/share/nginx/html

ENV API_BASE_URL=http://localhost:8080 \
    KEYCLOAK_URL=http://localhost:8081
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1
