FROM openjdk:17-jdk-slim
COPY app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
# This Dockerfile uses the OpenJDK 17 JDK slim image as the base image.