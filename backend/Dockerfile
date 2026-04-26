# Build stage
FROM gradle:8.6-jdk21-alpine AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src src

# Build the project
RUN gradle bootJar -x test

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
