# Sử dụng phiên bản nhẹ nhất có thể (17-jdk-slim ổn định hơn và nhẹ hơn openjdk:25-ea)
# FROM openjdk:17-jdk-slim
# FROM openjdk:17-jdk
FROM openjdk:25-ea-17-jdk

# Maintainer information
LABEL maintainer="Nguyen Quy <quyit.job@gmail.com>" \
      org.opencontainers.image.authors="Nguyen Quy <quyit.job@gmail.com>" \
      org.opencontainers.image.description="A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease."

USER root

# Thiết lập timezone và locale
ENV TZ=Asia/Ho_Chi_Minh \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8

# Cài wget, unzip, rồi cài Flyway CLI
RUN apt-get update && \
    apt-get install -y wget unzip && \
    wget -O flyway-commandline.tar.gz https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/8.5.13/flyway-commandline-8.5.13-linux-x64.tar.gz && \
    tar -xzf flyway-commandline.tar.gz && \
    mv flyway-*/flyway /usr/local/bin/flyway && \
    chmod +x /usr/local/bin/flyway

# Cấu hình Java runtime
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true --enable-native-access=ALL-UNNAMED" \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Chỉ định thư mục làm việc (tốt cho clarity và COPY)
WORKDIR /app

# Copy file JAR vào image
# COPY app.jar /app.jar
COPY app.jar ./app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose cổng mặc định của ứng dụng
EXPOSE 8081

# Lệnh chạy ứng dụng
# ENTRYPOINT ["java", "-jar", "/app.jar"]
# ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ./app.jar"]
ENTRYPOINT ["/entrypoint.sh"]
