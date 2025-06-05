FROM openjdk:17-jdk-slim

LABEL org.opencontainers.image.description="A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Cài đặt các font cần thiết & clean để giảm size image
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        fonts-dejavu \
        ttf-dejavu-core \
        libfreetype6 \
        libx11-6 \
        libxext6 \
        libxi6 \
        libxrender1 \
        libxtst6 && \
    rm -rf /var/lib/apt/lists/*

# Bật chế độ headless trong môi trường runtime
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

# Copy file JAR vào image
COPY app.jar /app.jar

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "/app.jar"]
