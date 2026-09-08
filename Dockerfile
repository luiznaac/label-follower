# syntax=docker/dockerfile:1

# ===========================================================================
# One image, two services:
#   - java -jar  (the Spring Boot backend)     -> ${API_PORT}, default 8080
#   - nginx      (the built SPA + /api proxy)  -> ${WEB_PORT}, default 8081
# supervised together by supervisord.
#
# The backend persists to flat files under /app/tooLazyToImplementPersistenceRightNow
# — mount a volume there if you want it to survive restarts.
# ===========================================================================

# ---------------------------------------------------------------------------
# Stage 1 — build the frontend SPA to static files
# ---------------------------------------------------------------------------
FROM node:22-slim AS frontend
WORKDIR /fe

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
# Served at the nginx root; API is reached through nginx's /api proxy
# (see deploy/nginx.conf.template), so the client's base stays "/api".
ENV VITE_BASE=/ \
    VITE_API_BASE=/api
RUN npm run build          # -> /fe/dist

# ---------------------------------------------------------------------------
# Stage 2 — build the backend jar
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app

COPY backend/ ./
# Strip any CRLF line endings a Windows checkout (core.autocrlf=true) may have
# introduced, so the wrapper's shebang resolves inside the Linux build stage.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar \
    && cp build/libs/*.jar app.jar

# ---------------------------------------------------------------------------
# Stage 3 — runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor gettext-base curl \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

ENV API_PORT=8080 \
    WEB_PORT=8081 \
    SPRING_PROFILES_ACTIVE=production

COPY --from=backend /app/app.jar /app/app.jar
COPY --from=frontend /fe/dist /var/www/label-follower
COPY deploy/nginx.conf.template /etc/nginx/templates/label-follower.conf.template
COPY deploy/supervisord.conf /etc/supervisor/conf.d/label-follower.conf
COPY deploy/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8080 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fsS "http://localhost:${WEB_PORT}/" >/dev/null || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
