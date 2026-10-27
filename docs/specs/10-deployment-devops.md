# Deployment & DevOps Specs
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Deployment Architecture

### 1.1 Development Environment
- **Operating System**: Windows, macOS, Linux
- **Java Version**: OpenJDK 17
- **Build Tool**: Maven 3.8.8+
- **Database**: H2 (file-based at `./data/mockwebhook`)
- **Port**: 8081 (configurable via `server.port`)
- **H2 Console**: Available at `/h2-console` (development only)

### 1.2 Production Environment
- **Operating System**: Linux (Alpine-based Docker image)
- **Java Version**: OpenJDK 17 JRE
- **Web Server**: Nginx (reverse proxy)
- **Database**: H2 (file-based or external PostgreSQL)
- **Port**: 8081 (internal), 443 (external via Nginx)
- **SSL/TLS**: Let's Encrypt certificates

### 1.3 Container Runtime
- **Docker**: Version 20.10+
- **Docker Compose**: Version 2.0+
- **Kubernetes**: Version 1.24+ (optional)

---

## 2. Build and Release Process

### 2.1 Build Pipeline
```yaml
name: Java CI/CD with Maven and Docker

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      - name: Build with Maven
        run: ./mvnw clean package -DskipTests
      - name: Run tests
        run: ./mvnw test
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: webapp
          path: target/WebHookMock-0.0.1-SNAPSHOT.jar

  docker:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      - name: Login to GHCR
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ghcr.io/nguyenquy0710/mockapi:latest
```

### 2.2 Release Process
1. **Version Bump**: Update version in `pom.xml`
2. **Changelog**: Update `CHANGELOG.md`
3. **Tag**: Create Git tag (e.g., `v1.0.2`)
4. **Build**: Run `./mvnw clean package`
5. **Deploy**: Push JAR to release assets
6. **Docker**: Build and push Docker image

### 2.3 Versioning Strategy
- **Format**: `MAJOR.MINOR.PATCH`
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

---

## 3. Docker Deployment

### 3.1 Dockerfile
```dockerfile
FROM openjdk:17-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```

### 3.2 Docker Compose
```yaml
version: '3.8'
services:
  web:
    image: ghcr.io/nguyenquy0710/mockapi:latest
    ports:
      - "8081:8081"
    volumes:
      - ./data:/app/data
    environment:
      - SPRING_PROFILES_ACTIVE=production
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

### 3.3 Running with Docker
```bash
# Pull image
docker pull ghcr.io/nguyenquy0710/mockapi:latest

# Run container
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  --name mockapi \
  ghcr.io/nguyenquy0710/mockapi:latest

# View logs
docker logs -f mockapi

# Stop container
docker stop mockapi

# Remove container
docker rm mockapi
```

---

## 4. Nginx Configuration

### 4.1 Production Nginx Config
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

### 4.2 SSL/TLS Configuration
- **Protocol**: TLS 1.2+
- **Ciphers**: Modern cipher suite
- **HSTS**: Enabled with 1 year max-age
- **OCSP Stapling**: Enabled

---

## 5. Kubernetes Deployment (Optional)

### 5.1 Deployment Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mockapi
  labels:
    app: mockapi
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mockapi
  template:
    metadata:
      labels:
        app: mockapi
    spec:
      containers:
      - name: mockapi
        image: ghcr.io/nguyenquy0710/mockapi:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        volumeMounts:
        - name: data-volume
          mountPath: /app/data
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: data-volume
        persistentVolumeClaim:
          claimName: mockapi-data-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: mockapi-service
spec:
  selector:
    app: mockapi
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8081
  type: LoadBalancer
```

---

## 6. CI/CD Integration

### 6.1 GitHub Actions Workflow
- **Trigger**: Push to main/develop, pull requests
- **Build**: Maven clean package
- **Test**: JUnit tests with coverage
- **Deploy**: Docker image to GHCR
- **Notify**: Slack/Email on success/failure

### 6.2 Automated Testing
- **Unit Tests**: Run on every build
- **Integration Tests**: Run on pull requests
- **Security Scans**: OWASP Dependency Check
- **Code Quality**: SonarQube analysis

### 6.3 Deployment Strategies
- **Blue-Green**: Zero-downtime deployments
- **Rolling Update**: Gradual rollout
- **Canary**: Gradual feature rollout to subset of users

---

## 7. Monitoring and Logging

### 7.1 Application Logging
- **Framework**: SLF4J + Logback
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Log Format**: JSON for structured logging
- **Log Location**: `/app/logs/` (Docker) or console

### 7.2 Health Checks
- **Endpoint**: `/actuator/health`
- **Database**: H2 connection check
- **Disk Space**: Minimum 1GB available
- **Memory**: Alert if > 80% usage

### 7.3 Metrics
- **Request Count**: Total requests per endpoint
- **Response Time**: P50, P95, P99
- **Database Queries**: Query count and duration
- **WebSocket Connections**: Active connections

### 7.4 Alerting
- **Error Rate**: > 5% in 5 minutes
- **Response Time**: P99 > 5 seconds
- **Database**: Connection failures
- **Disk Space**: < 1GB available

---

## 8. Backup and Recovery

### 8.1 Database Backup
```bash
# Backup H2 database
cp ./data/mockwebhook.mv.db ./backup/mockwebhook-$(date +%Y%m%d).mv.db

# Restore H2 database
cp ./backup/mockwebhook-20240101.mv.db ./data/mockwebhook.mv.db
```

### 8.2 Automated Backup
- **Frequency**: Daily at 02:00 UTC
- **Retention**: 30 days
- **Storage**: Object storage (S3, GCS, etc.)
- **Encryption**: AES-256

### 8.3 Disaster Recovery
1. **Database Corruption**: Restore from latest backup
2. **Application Failure**: Restart container
3. **Infrastructure Failure**: Deploy to new server
4. **Data Loss**: Restore from backup + replay logs

---

## 9. Security Best Practices

### 9.1 Secrets Management
- **Environment Variables**: Database credentials
- **Secrets Manager**: AWS Secrets Manager, HashiCorp Vault
- **Never Commit**: API keys, passwords, tokens

### 9.2 Network Security
- **HTTPS**: All production traffic
- **CORS**: Origin whitelist
- **Rate Limiting**: Per-user rate limiting
- **IP Whitelist**: Admin endpoints only

### 9.3 Application Security
- **Input Validation**: All user inputs
- **SQL Injection**: Parameterized queries
- **XSS**: Output encoding
- **CSRF**: Tokens for forms

---

## 10. Troubleshooting

### 10.1 Common Issues

#### Database Connection Failed
```bash
# Check H2 database file
ls -la ./data/

# Repair database
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=

# Or delete data directory (fresh start)
rm -rf ./data/
```

#### Application Won't Start
```bash
# Check logs
docker logs mockapi

# Check Java version
java -version

# Check memory
docker stats mockapi
```

#### Port Already in Use
```bash
# Find process using port 8081
lsof -i :8081

# Change port in application.properties
server.port=8082
```

### 10.2 Debug Mode
```bash
# Enable debug logging
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.org.springframework=DEBUG"

# Enable remote debugging
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

---

## 11. Performance Tuning

### 11.1 JVM Options
```bash
# Production JVM options
java -jar app.jar \
  -Xms512m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof
```

### 11.2 Database Optimization
- **Connection Pool**: HikariCP (default)
- **Pool Size**: 10 connections
- **Query Cache**: Enable Hibernate second-level cache

### 11.3 Caching Strategy
- **HTTP Caching**: Cache static assets
- **Response Caching**: Cache API responses (optional)
- **Session Caching**: Redis for distributed sessions (optional)

---

## 12. Environment Configuration

### 12.1 Development
```properties
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.show-sql=true
spring.h2.console.enabled=true
app.domain=http://localhost:8081
```

### 12.2 Production
```properties
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.username=sa
spring.datasource.password=${DB_PASSWORD}
spring.jpa.show-sql=false
spring.h2.console.enabled=false
app.domain=https://mock-api.autobot.site
```

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
