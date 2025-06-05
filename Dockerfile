# FROM openjdk:17-jdk-slim
FROM openjdk:17-jdk

LABEL org.opencontainers.image.description "A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Bật chế độ headless trong môi trường runtime
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

# Copy file JAR vào image
COPY app.jar /app.jar

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "/app.jar"]
