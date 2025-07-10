# Sử dụng phiên bản nhẹ nhất có thể (17-jdk-slim ổn định hơn và nhẹ hơn openjdk:25-ea)
# FROM openjdk:17-jdk-slim
# FROM openjdk:17-jdk
FROM openjdk:25-ea-17-jdk

# Maintainer information
LABEL maintainer="Nguyen Quy <quy.nh@nhquydev.net>" \
      org.opencontainers.image.authors="Nguyen Quy <quy.nh@nhquydev.net>" \
      org.opencontainers.image.description="A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Thiết lập timezone và locale
ENV TZ=Asia/Ho_Chi_Minh \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8

# Cài đặt tzdata để hỗ trợ timezone + cleanup layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Cấu hình Java runtime
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true" \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Chỉ định thư mục làm việc (tốt cho clarity và COPY)
WORKDIR /app

# Copy file JAR vào image
# COPY app.jar /app.jar
COPY app.jar ./app.jar

# Expose cổng mặc định của ứng dụng
EXPOSE 8081

# Lệnh chạy ứng dụng
# ENTRYPOINT ["java", "-jar", "/app.jar"]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ./app.jar"]
