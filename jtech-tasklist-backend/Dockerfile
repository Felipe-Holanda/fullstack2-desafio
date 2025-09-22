# Multi-stage build for Spring Boot (Java 21)

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Leverage layer caching for dependencies
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

# Build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user and data directory for H2 file
RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/build/data \
    && chown -R app:app /app

# Copy fat jar
COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080
USER app

# H2 file path is configured to ./build/data in application.properties
ENTRYPOINT ["java","-jar","/app/app.jar"]
