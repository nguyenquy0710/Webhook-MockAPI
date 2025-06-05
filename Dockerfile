FROM openjdk:17-jdk-slim

LABEL org.opencontainers.image.description "A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Cài font cần thiết
RUN apt-get update && \
    apt-get install -y fonts-dejavu ttf-dejavu openjdk-17-jdk-headless && \
    apt-get clean

# Đảm bảo headless được bật
ENV JAVA_OPTS="-Djava.awt.headless=true"

COPY app.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
# This Dockerfile uses the OpenJDK 17 JDK slim image as the base image.