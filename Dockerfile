# Build stage
FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package

# Runtime stage
FROM openjdk:17-jdk-slim
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]