# EventFlow Backend - Multi-stage Docker build
# Stage 1: Build with Maven and JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM files first for dependency caching
COPY mvnw pom.xml ./
COPY .mvn .mvn/

# Download dependencies (cached layer unless POM changes)
RUN ./mvnw dependency:go-offline -B -q

# Copy source code
COPY src src/

# Build the application (skip tests for production build)
RUN ./mvnw package -DskipTests -B -q

# Stage 2: Minimal runtime image with distroless base
FROM eclipse-temurin:21-jre-alpine AS runtime

# Add non-root user for security
RUN addgroup -S eventflow && adduser -S eventflow -G eventflow

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R eventflow:eventflow /app

# Switch to non-root user
USER eventflow

# Configure JVM for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

# Expose application port
EXPOSE 8080

# Start the application
ENTRYPOINT exec java ${JAVA_OPTS} -jar /app/app.jar