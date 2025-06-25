# FROM openjdk:17-jdk-slim
# FROM openjdk:17-jdk
FROM openjdk:25-ea-17-jdk

# Maintainer information
LABEL maintainer="Nguyen Quy <quy.nh@nhquydev.net>"
LABEL org.opencontainers.image.authors="Nguyen Quy <quy.nh@nhquydev.net>"
LABEL org.opencontainers.image.description="A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Cài đặt múi giờ và locale
ENV TZ=Asia/Ho_Chi_Minh \
  LANG=en_US.UTF-8 \
  LANGUAGE=en_US.UTF-8

# Cấu hình Java runtime
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Mặc định ứng dụng chạy cổng 8081
EXPOSE 8081

# Copy file JAR vào image
COPY app.jar /app.jar

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "/app.jar"]
