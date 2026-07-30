# EventFlow Backend - Multi-stage Docker build
# Stage 1: Build with Maven and JDK 17
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy POM first for dependency caching
COPY pom.xml ./

# Download dependencies (cached layer unless POM changes)
RUN mvn dependency:go-offline -B -q

# Copy source code
COPY src src/

# Copy Avro schemas if they exist
COPY src/main/resources/avro src/main/resources/avro/

# Build the application (skip tests for production build)
RUN mvn package -DskipTests -B -q

# Stage 2: Minimal runtime image with JRE 17
FROM eclipse-temurin:17-jre-alpine AS runtime

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