[![Java CI/CD with Maven and Docker](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml)

# WebHookMock

A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease.

## 📖 Overview

WebHookMock is a Spring Boot application designed to simplify webhook development and testing. It provides a seamless way to:

- Create custom API endpoints with configurable responses
- Monitor and log incoming webhook requests in real-time
- Configure various response parameters including status codes, headers, and response bodies
- Add intentional delays to simulate real-world API behavior
- Export request logs for documentation and analysis

## ✨ Features

### ✅ API Configuration

- Create custom endpoints with personalized paths
- Configure HTTP response status codes (200, 201, 400, 404, etc.)
- Set response content types (JSON, XML, HTML, etc.)
- Customize response headers
- Add response delays to simulate latency
- Dynamic response templates with request variables (e.g. `{{headers.User-Agent}}`)

### 📡 Request Monitoring

- Real-time logging of all incoming requests
- Detailed view of request headers, body, and parameters
- WebSocket support for live updates
- Export logs to Excel for further analysis
- Each log entry includes a ready-to-use `curl` command
- REST endpoints documented in `static /swagger.json`
- Swagger UI available at `/swagger-ui` and per-user specs at `/swagger/@{username}`

### 🔐 User Management

- Secure registration and authentication
- User-specific webhook URLs (e.g., `https://domain/api/@username/path`)
- Isolated environments for each user

## 🧰 Technology Stack

- **Backend**: Spring Boot 2.7.9
- **Security**: Spring Security
- **Database**: H2 Database (file-based persistence)
- **Frontend**: Thymeleaf, Bootstrap 5, Font Awesome
- **Real-time Updates**: WebSocket (STOMP)
- **Export Functionality**: Apache POI (Excel)

## 🚀 Getting Started

### Prerequisites

- Java 17
- Maven 3.6+ (or use the included Maven wrapper)

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application using Maven:

```bash
# Cấu hình môi trường Java 17
export JAVA_HOME=/home/codespace/java/17.0.15-ms
export PATH=$JAVA_HOME/bin:$PATH

java -version
javac -version

# Xóa thư mục target (build cũ)
./mvnw clean

# Build và cài đặt vào local repo
./mvnw install

# Build ra file JAR hoặc WAR trong target/
./mvnw package

# Chạy lệnh flyway repair để sửa lại lịch sử migration
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=

# Chạy ứng dụng Spring Boot
./mvnw spring-boot:run
# hoặc
java -jar ./app.jar
java -jar ./target/WebHookMock-0.0.1-SNAPSHOT.jar

# Chạy test unit
./mvnw test

# Xem cây dependency
./mvnw dependency:tree
```

Or on Windows:

```bash
mvnw.cmd spring-boot:run
```

4. Access the application at `http://localhost:8081`

### 🐳 Docker Deployment

#### Pull from GHCR

```bash
docker pull ghcr.io/nguyenquy0710/mockapi:latest
```

#### Docker Compose

Use the included Docker Compose file to run the app quickly: [docker-compose.yml](/docker-compose.yml)

```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
docker-compose up -d
```

Default URL: `http://localhost:8081`

### 📦 Release Info

- Version: [1.0.2](https://github.com/nguyenquy0710/Webhook-MockAPI/releases/tag/1.0.2)
- Release Date: 2025-06-05
- Stability: Release Candidate (RC)

### 🔄 What's New

- Docker & Docker Compose support
- Live WebSocket-based request logs
- Export to Excel via Apache POI
- Modern UI with Bootstrap 5
- Isolated user environments via URL namespace

### Configuration

The application uses an H2 database by default, with the database file stored in the `./data/mockwebhook` directory. You can modify the configuration in `application.properties`.
You can set the base domain for generated webhook URLs via `app.domain` or the `APP_DOMAIN` environment variable. The H2 console is available at `/h2-console` during development.

### Database Migration

**Important**: If you're upgrading from a previous version, your existing database will be automatically migrated to support new features.

The application now includes:

- **Flyway** for automatic database migration
- **Response delay functionality** - configure delays (0-60000ms) for API responses
- **Improved database versioning** and migration tracking

For detailed migration information, see [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md).

To validate your migration was successful, run:

```bash
./validate_migration.sh
```

### Troubleshooting Database Issues

If you encounter Flyway migration validation errors, use the repair script:

```bash
./repair_database.sh
```

This script will attempt to repair failed migrations automatically. If issues persist, you may need to delete the `data/` directory to start with a fresh database.

## Usage

1. **Register an account**: Create a personal account to get your unique webhook URL
2. **Create API configurations**: Define custom endpoints with your desired response data
3. **Use your webhook**: Send requests to your personalized webhook URL
4. **Monitor requests**: View detailed logs of all incoming requests
5. **Export data**: Export request logs to Excel for further analysis

## Project Structure

- `src/main/java/vn/autobot/webhook/` - Java source code
  - `config/` - Application configuration classes
  - `controller/` - REST and MVC controllers
  - `dto/` - Data transfer objects
  - `model/` - Entity classes
  - `repository/` - Data access layer
  - `service/` - Business logic
- `src/main/resources/` - Application resources
  - `static/` - CSS, JavaScript, and other static assets
  - `templates/` - Thymeleaf templates
  - `application.properties` - Application configuration

## Security

The application implements standard Spring Security features:

- Password encryption using BCrypt
- Form-based authentication
- Request path authorization
- CSRF protection (disabled for webhook APIs for easier integration)

## Nginx Config

```
server {
    server_name mock-api.autobot.site www.mock-api.autobot.site;
    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_headers_hash_max_size 512;
        proxy_headers_hash_bucket_size 128;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Server $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;  # Forward the Upgrade header
        proxy_set_header Connection 'upgrade';  # Forward the Connection header
        proxy_set_header Referer $http_referer;
        proxy_hide_header Access-Control-Allow-Origin;
        add_header Access-Control-Allow-Origin * always;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10m;
    }


    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/mock-api.autobot.site/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/mock-api.autobot.site/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}
server {
    if ($host = mock-api.autobot.site) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    server_name mock-api.autobot.site www.mock-api.autobot.site;
    listen 80;
    return 404; # managed by Certbot
}
```

## Github Codespaces

- Nếu bạn dùng **SDKMAN**

  ```bash
  sdk list java

  sdk use java 17.0.14-ms
  ```

- Kiểm tra phiên bản hiện tại
  ```bash
  java -version
  javac -version
  ```
- Kiểm tra danh sách phiên bản đang có:
  ```bash
  ls -la /home/codespace/java/
  ```
- Export thủ công
  ```bash
  export JAVA_HOME=/home/codespace/java/17.0.15-ms
  export PATH=$JAVA_HOME/bin:$PATH
  ```
- Tự động dùng JDK 17 mỗi lần mở Codespace

  ```bash
  echo 'export JAVA_HOME=/home/codespace/java/17.0.15-ms' >> ~/.bashrc
  echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

  # Rồi chạy:
  source ~/.bashrc
  ```

## Author

- Developed by: nchuyen128@gmail.com FROM https://autobot.site
- Developed by: quyit.job@gmail.com FROM https://blogs.nhquydev.net

## 📚 Documentation

### For Users
- **[Installation Guide](./docs/INSTALLATION.md)** - How to install and set up WebHookMock
- **[User Guide](./docs/USER-GUIDE.md)** - Complete guide for using WebHookMock features
- **[API Documentation](./docs/API-DOCUMENTATION.md)** - REST API endpoints and usage

### For Developers
- **[Architecture](./docs/ARCHITECTURE.md)** - System architecture and design patterns
- **[Development Guide](./docs/DEVELOPMENT.md)** - Setting up development environment
- **[Testing Guide](./docs/TESTING.md)** - Running and writing tests
- **[Database Migration](./docs/DATABASE-MIGRATION.md)** - Database schema and migrations

### For DevOps
- **[Deployment Guide](./docs/DEPLOYMENT.md)** - Deploying to production
- **[Docker Deployment](./docs/DOCKER-DEPLOYMENT.md)** - Running with Docker
- **[CI/CD Pipeline](./docs/CI-CD.md)** - Continuous integration and deployment

### Additional Resources
- **[Security](./docs/SECURITY.md)** - Security best practices
- **[Contributing](./docs/CONTRIBUTING.md)** - How to contribute

## Changelog

All notable changes to this project will be recorded in the [CHANGELOG.md](/CHANGELOG.md) file.

---

**👉 Đóng góp hoặc liên hệ:**
Hãy mở issue hoặc pull request nếu bạn có đề xuất nâng cấp hoặc phát hiện lỗi.

<p style="text-align: center;"> © 2025 WebHookMock. All rights reserved. </p>
