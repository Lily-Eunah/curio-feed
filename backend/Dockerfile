# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy gradle wrapper and config files
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

# Fix CRLF line endings for Alpine Linux and make executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy source
COPY src src

# Run gradle build, skipping tests and disabling daemon to save memory
RUN ./gradlew bootJar -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
