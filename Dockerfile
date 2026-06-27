# syntax=docker/dockerfile:1

# ---- Build stage: compile the Spring Boot bootJar with the Gradle wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :api:bootJar --no-daemon

# ---- Runtime stage: slim JRE running the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api/build/libs/app.jar app.jar
# Platforms map this; the app also reads $PORT (see application.yml).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
