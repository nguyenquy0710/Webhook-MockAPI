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

### 📡 Request Monitoring
- Real-time logging of all incoming requests
- Detailed view of request headers, body, and parameters
- WebSocket support for live updates
- Export logs to Excel for further analysis

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
./mvnw spring-boot:run
```

Or on Windows:

```bash
mvnw.cmd spring-boot:run
```

4. Access the application at `http://localhost:8081`

### 🐳 Docker Deployment

#### Pull from GHCR

```bash
docker pull ghcr.io/nguyenquy0710/mockapi:1.0.0-rc13
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

- Version: 1.0.0-rc13
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

## Author

Developed by: nchuyen128@gmail.com FROM https://autobot.site

---

© 2025 WebHookMock. All rights reserved.
