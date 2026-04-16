# Installation Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Yêu cầu hệ thống / System Requirements](#yêu-cầu-hệ-thống--system-requirements)
- [Cài đặt môi trường phát triển / Development Environment Setup](#cài-đặt-môi-trường-phát-triển--development-environment-setup)
- [Build và chạy ứng dụng / Build and Run](#build-và-chạy-ứng-dụng--build-and-run)
- [Cấu hình cơ sở dữ liệu / Database Configuration](#cấu-hình-cơ-sở-dữ-liệu--database-configuration)

---

## Yêu cầu hệ thống / System Requirements

### Required Software
| Software | Version | Description |
|----------|---------|-------------|
| Java | 17+ | JDK 17 or higher |
| Maven | 3.8.8+ | Build tool |
| Git | Latest | Version control |

### Optional
| Software | Version | Description |
|----------|---------|-------------|
| Docker | 20.10+ | Container runtime |
| Docker Compose | 2.0+ | Container orchestration |

### Kiểm tra phiên bản / Check Versions
```bash
# Java version
java -version
javac -version

# Maven version
mvn --version

# Git version
git --version
```

---

## Cài đặt môi trường phát triển / Development Environment Setup

### Bước 1: Clone repository / Clone Repository
```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
```

### Bước 2: Cấu hình Java / Configure Java
```bash
# Kiểm tra Java version
java -version

# Nếu cần cài đặt Java 17 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-17-jdk

# Nếu cần cài đặt Java 17 (macOS với Homebrew)
brew install openjdk@17

# Nếu cần cài đặt Java 17 (Windows)
# Download from: https://adoptium.net/
```

### Bước 3: Cài đặt Maven (nếu chưa có) / Install Maven
```bash
# Ubuntu/Debian
sudo apt install maven

# macOS với Homebrew
brew install maven

# Windows
# Download from: https://maven.apache.org/download.cgi
```

---

## Build và chạy ứng dụng / Build and Run

### Build ứng dụng / Build Application
```bash
# Build JAR file
./mvnw clean package

# Hoặc trên Windows
mvnw.cmd clean package
```

### Chạy ứng dụng / Run Application
```bash
# Chạy với Maven wrapper (Linux/macOS)
./mvnw spring-boot:run

# Chạy với Maven wrapper (Windows)
mvnw.cmd spring-boot:run

# Hoặc chạy JAR file trực tiếp
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar
```

### Truy cập ứng dụng / Access Application
- **URL**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:file:./data/mockwebhook`
  - Username: `sa`
  - Password: `password`

---

## Cấu hình cơ sở dữ liệu / Database Configuration

### Mặc định / Default Configuration
WebHookMock sử dụng H2 database file-based tại `./data/mockwebhook`:

```properties
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

### Cấu hình Flyway Migration
Flyway tự động quản lý database migrations:

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

### Kiểm tra migration / Verify Migration
```bash
# Chạy migration validation script
./validate_migration.sh

# Hoặc trên Windows
.\validate_migration.ps1
```

### Sửa lỗi migration / Repair Migration
```bash
# Chạy repair script
./repair_database.sh

# Hoặc trên Windows
.\repair_database.ps1

# Hoặc chạy Flyway repair thủ công
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=
```

### Reset database (nếu cần) / Reset Database
```bash
# Xóa thư mục data (cẩn thận - mất tất cả dữ liệu)
rm -rf ./data/

# Hoặc trên Windows
rmdir /s /q .\data\
```

---

## Troubleshooting

### Lỗi: "Java version not supported"
```bash
# Kiểm tra Java version
java -version

# Cài đặt Java 17
# Ubuntu: sudo apt install openjdk-17-jdk
# macOS: brew install openjdk@17
```

### Lỗi: "Maven not found"
```bash
# Cài đặt Maven
# Ubuntu: sudo apt install maven
# macOS: brew install maven
```

### Lỗi: "Database migration failed"
```bash
# Chạy repair script
./repair_database.sh

# Hoặc xóa data directory
rm -rf ./data/
```

### Lỗi: "Port 8081 already in use"
```bash
# Thay đổi port trong application.properties
server.port=8082
```

---

## Next Steps

- [ ] [Development Guide](./DEVELOPMENT.md) - Setting up development environment
- [ ] [Docker Deployment](./DOCKER-DEPLOYMENT.md) - Running with Docker
- [ ] [User Guide](./USER-GUIDE.md) - Using WebHookMock features

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [H2 Database Documentation](https://www.h2database.com)
- [Flyway Documentation](https://flywaydb.org/documentation)
- [Maven Documentation](https://maven.apache.org/guides/)
