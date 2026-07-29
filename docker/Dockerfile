# EventFlow Backend Dockerfile
# Multi-stage build for production-ready, minimal image

# Stage 1: Build with Maven and JDK
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and dependency files for layer caching
COPY pom.xml .
COPY src ./src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -q

# Stage 2: Runtime with Distroless Java
FROM gcr.io/distroless/java21-debian12:nonroot

# Copy the JAR from builder
COPY --from=builder /workspace/target/eventflow-backend-*.jar /app/eventflow.jar

# Security: run as non-root user
USER nonroot:nonroot

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD ["java", "-cp", "/app/eventflow.jar", "org.springframework.boot.actuate.health.HealthEndpoint"]

# JVM tuned for containers
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseZGC", \
    "-XX:ZCollectionInterval=30", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-jar", \
    "/app/eventflow.jar"]