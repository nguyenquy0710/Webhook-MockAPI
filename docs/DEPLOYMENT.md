# Deployment Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Standalone JAR Deployment](#standalone-jar-deployment)
- [Docker Deployment](#docker-deployment)
- [Docker Compose Deployment](#docker-compose-deployment)
- [Production Configuration](#production-configuration)
- [CI/CD Pipeline](#cicd-pipeline)
- [Nginx Reverse Proxy](#nginx-reverse-proxy)
- [Monitoring and Logging](#monitoring-and-logging)
- [Backup and Recovery](#backup-and-recovery)

---

## Standalone JAR Deployment

### Build JAR
```bash
./mvnw clean package -DskipTests
```

### Run JAR
```bash
# Basic run
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar

# With custom port
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar --server.port=8082

# With custom config
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar --spring.config.location=custom-config.properties
```

### Run as Service (Linux)

Create systemd service file:
```bash
sudo nano /etc/systemd/system/webhook-mock.service
```

```ini
[Unit]
Description=WebHookMock Service
After=network.target

[Service]
User=webhook
WorkingDirectory=/opt/webhook-mock
ExecStart=/usr/bin/java -jar /opt/webhook-mock/WebHookMock-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
Environment=JAVA_OPTS=-Xmx512m -Xms256m

[Install]
WantedBy=multi-user.target
```

```bash
# Reload systemd
sudo systemctl daemon-reload

# Start service
sudo systemctl start webhook-mock

# Enable on boot
sudo systemctl enable webhook-mock

# Check status
sudo systemctl status webhook-mock
```

---

## Docker Deployment

### Build Docker Image
```bash
# Build image
docker build -t webhook-mock:latest .

# Tag for registry
docker tag webhook-mock:latest ghcr.io/nguyenquy0710/mockapi:latest
```

### Run Container
```bash
# Basic run
docker run -d -p 8081:8081 webhook-mock:latest

# With volume for data persistence
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  webhook-mock:latest

# With custom environment variables
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  -e SPRING_PROFILES_ACTIVE=production \
  -e APP_DOMAIN=https://your-domain.com \
  webhook-mock:latest
```

### Pull from Registry
```bash
docker pull ghcr.io/nguyenquy0710/mockapi:latest
```

---

## Docker Compose Deployment

### Quick Start
```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  mockapi:
    image: ghcr.io/nguyenquy0710/mockapi:latest
    container_name: mockapi
    restart: unless-stopped
    ports:
      - "8081:8081"
    environment:
      - TZ=Asia/Ho_Chi_Minh
      - SPRING_PROFILES_ACTIVE=default
      - APP_DOMAIN=https://your-domain.com
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
    deploy:
      resources:
        limits:
          cpus: "0.50"
          memory: "2G"
        reservations:
          memory: "256M"
```

---

## Production Configuration

### application.properties
```properties
# Server Configuration
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.username=sa
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=validate

# Flyway Configuration
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# H2 Console (disable in production)
spring.h2.console.enabled=false

# Thymeleaf (enable cache in production)
spring.thymeleaf.cache=true

# Logging
logging.level.root=INFO
logging.level.org.springframework.web=INFO
logging.file.name=logs/app.log

# App Configuration
app.domain=https://your-domain.com
app.request-log-retention-days=30
```

### Environment Variables
```bash
# Database
DB_PASSWORD=your-secure-password

# Application
APP_DOMAIN=https://your-domain.com
SPRING_PROFILES_ACTIVE=production

# Java Options
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
```

---

## CI/CD Pipeline

### GitHub Actions Workflow
```yaml
name: Java CI/CD with Maven and Docker

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  release:
    types: [created]

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Build with Maven
        run: ./mvnw clean package -DskipTests
      - name: Run tests
        run: ./mvnw test

  docker-deploy:
    needs: build-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        run: |
          docker build -t ghcr.io/nguyenquy0710/mockapi:${{ env.VERSION }} \
                       -t ghcr.io/nguyenquy0710/mockapi:latest .
      - name: Push Docker image
        run: |
          docker push ghcr.io/nguyenquy0710/mockapi:${{ env.VERSION }}
          docker push ghcr.io/nguyenquy0710/mockapi:latest
```

### Release Process
1. Create Git tag: `git tag v1.0.0`
2. Push tag: `git push origin v1.0.0`
3. GitHub Actions builds and deploys automatically

---

## Nginx Reverse Proxy

### Nginx Configuration
```nginx
upstream mockapi {
    server localhost:8081;
}

server {
    server_name mock-api.autobot.site www.mock-api.autobot.site;
    
    location / {
        proxy_pass http://mockapi;
        proxy_set_header Host $host;
        proxy_headers_hash_max_size 512;
        proxy_headers_hash_bucket_size 128;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Server $host;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Referer $http_referer;
        proxy_hide_header Access-Control-Allow-Origin;
        add_header Access-Control-Allow-Origin * always;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10m;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/mock-api.autobot.site/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mock-api.autobot.site/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}

server {
    if ($host = mock-api.autobot.site) {
        return 301 https://$host$request_uri;
    }
    server_name mock-api.autobot.site www.mock-api.autobot.site;
    listen 80;
    return 404;
}
```

### SSL/TLS with Let's Encrypt
```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Obtain certificate
sudo certbot --nginx -d mock-api.autobot.site -d www.mock-api.autobot.site

# Auto-renew
sudo certbot renew
```

---

## Monitoring and Logging

### Application Logging
```properties
# Log levels
logging.level.root=INFO
logging.level.org.springframework.web=INFO
logging.level.vn.autobot.webhook=DEBUG

# Log file
logging.file.name=logs/app.log

# Log format
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Health Check
```bash
# Check application health
curl http://localhost:8081/actuator/health

# Check database connection
curl http://localhost:8081/actuator/health/database
```

### Metrics (with Actuator)
```properties
# Enable Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

---

## Backup and Recovery

### Database Backup
```bash
# Backup H2 database
cp ./data/mockwebhook.mv.db ./backup/mockwebhook-$(date +%Y%m%d).mv.db

# Or use Flyway backup
./mvnw flyway:backup -Dflyway.url=jdbc:h2:file:./data/mockwebhook
```

### Automated Backup Script
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="./backup"
DB_PATH="./data/mockwebhook"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory if not exists
mkdir -p $BACKUP_DIR

# Backup database
cp ${DB_PATH}.mv.db ${BACKUP_DIR}/mockwebhook_${DATE}.mv.db

# Keep only last 30 days of backups
find $BACKUP_DIR -name "mockwebhook_*.mv.db" -mtime +30 -delete

echo "Backup completed: mockwebhook_${DATE}.mv.db"
```

### Database Recovery
```bash
# Restore from backup
cp ./backup/mockwebhook-20240101.mv.db ./data/mockwebhook.mv.db

# Repair Flyway history if needed
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook
```

### Docker Backup
```bash
# Backup Docker volume
docker run --rm -v mockapi_data:/data -v $(pwd):/backup ubuntu tar czf /backup/backup.tar.gz /data

# Restore from backup
docker run --rm -v mockapi_data:/data -v $(pwd):/backup ubuntu tar xzf /backup/backup.tar.gz -C /
```

---

## Security Best Practices

### Production Checklist
- [ ] Disable H2 console in production
- [ ] Use strong database password
- [ ] Enable HTTPS with valid SSL certificate
- [ ] Set up firewall rules
- [ ] Configure rate limiting
- [ ] Enable security headers
- [ ] Set up monitoring and alerting
- [ ] Configure automated backups
- [ ] Enable audit logging
- [ ] Regular security scans

### Security Headers (Nginx)
```nginx
# Security headers
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'" always;
```

---

## Troubleshooting

### Container Won't Start
```bash
# Check logs
docker logs mockapi

# Check Java version
docker exec mockapi java -version

# Check memory
docker stats mockapi
```

### Database Connection Failed
```bash
# Check H2 database file
ls -la ./data/

# Repair database
./repair_database.sh

# Or delete data directory (fresh start)
rm -rf ./data/
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8081

# Change port in application.properties
server.port=8082
```

### SSL Certificate Expired
```bash
# Renew certificate
sudo certbot renew

# Restart Nginx
sudo systemctl restart nginx
```

---

## Next Steps

- [ ] [User Guide](./USER-GUIDE.md) - Using WebHookMock features
- [ ] [Architecture](./ARCHITECTURE.md) - System design
- [ ] [CI/CD Pipeline](./CI-CD.md) - Continuous integration
