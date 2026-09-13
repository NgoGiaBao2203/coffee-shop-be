# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /build/target/api-0.0.1-SNAPSHOT.jar ./app.jar

# Change ownership of the app
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Run the application with prod profile
# Required environment variables:
# - DB_HOST: Database host
# - DB_PORT: Database port
# - DB_NAME: Database name
# - DB_USERNAME: Database username
# - DB_PASSWORD: Database password
# - SUPABASE_BUCKET_NAME: S3 bucket name
# - SUPABASE_REGION: AWS region
# - SUPABASE_ENDPOINT: Supabase endpoint URL
# - SUPABASE_ACCESS_KEY: S3 access key
# - SUPABASE_SECRET_KEY: S3 secret key
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
