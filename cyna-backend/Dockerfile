# syntax=docker/dockerfile:1

# ---------- Stage 1: build the Spring Boot fat jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Warm the Gradle dependency cache on its own layer so source-only changes
# don't re-download the world.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
# Normalize CRLF (the wrapper may be checked out with Windows line endings,
# which would make the shebang unexecutable in Linux -> exit 127).
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Build the application (tests run in CI, not in the image build).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Stage 2: minimal runtime ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl for the container healthcheck; run as an unprivileged user.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system cyna && useradd --system --gid cyna --home /app cyna

COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar
# Writable dir for the JSON log files (logback LOG_PATH). Console JSON logs go
# to stdout regardless and are the primary sink in containers.
RUN mkdir -p /app/logs && chown -R cyna:cyna /app
USER cyna

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Actuator health endpoint is exposed (management.endpoints include=health).
# The prod profile enforces HTTPS (redirects plain HTTP), so the loopback
# healthcheck must present itself as forwarded-from-https to get a 200.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS -H "X-Forwarded-Proto: https" http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
