# Bước 1: Chọn image nền Java để build ứng dụng
FROM maven:3.8.4-openjdk-11-slim AS build

# Bước 2: Thiết lập thư mục làm việc trong container
WORKDIR /app

# Bước 3: Sao chép file pom.xml (hoặc build.gradle) và tải các dependencies
COPY pom.xml .

# Chạy Maven để tải về các dependencies
RUN mvn dependency:go-offline

# Bước 4: Sao chép toàn bộ mã nguồn vào container
COPY . .

# Bước 5: Build ứng dụng (package lại thành file .jar)
RUN mvn clean package -DskipTests

# Bước 6: Chọn image nền nhẹ để chạy ứng dụng
FROM openjdk:11-jre-slim

# Bước 7: Thiết lập thư mục làm việc cho ứng dụng
WORKDIR /app

# Bước 8: Sao chép file JAR từ bước build vào container
COPY --from=build /app/target/your-app.jar /app/your-app.jar

# Bước 9: Chạy ứng dụng Java (thay đổi tên file JAR nếu cần)
ENTRYPOINT ["java", "-jar", "/app/your-app.jar"]

# Bước 10: Mở cổng ứng dụng (ví dụ: 8080 cho Spring Boot)
EXPOSE 8080
