# Docker Deployment Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Docker Compose](#docker-compose)
- [Custom Configuration](#custom-configuration)
- [Volume Mounts](#volume-mounts)
- [Environment Variables](#environment-variables)
- [Building Custom Images](#building-custom-images)
- [Troubleshooting](#troubleshooting)

---

## Overview

### Docker Images
- **Registry**: GitHub Container Registry (GHCR)
- **Image**: `ghcr.io/nguyenquy0710/mockapi`
- **Tags**: `latest`, `v1.0.0`, etc.

### Dockerfile
```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:25-ea-17-jdk
USER root
ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"
WORKDIR /app
COPY --from=build /app/target/WebHookMock-0.0.1-SNAPSHOT.jar ./app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ./app.jar"]
```

---

## Prerequisites

### Required Software
| Software | Version | Description |
|----------|---------|-------------|
| Docker | 20.10+ | Container runtime |
| Docker Compose | 2.0+ | Container orchestration |

### Verify Installation
```bash
# Docker version
docker --version

# Docker Compose version
docker-compose --version
```

---

## Quick Start

### Pull and Run
```bash
# Pull image from GHCR
docker pull ghcr.io/nguyenquy0710/mockapi:latest

# Run container
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Access Application
- **URL**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:file:/app/data/mockwebhook`
  - Username: `sa`
  - Password: `password`

---

## Docker Compose

### Quick Start with docker-compose.yml
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

## Custom Configuration

### Custom application.properties
```bash
# Create custom config
mkdir -p ./config
cat > ./config/application.properties << EOF
server.port=8081
spring.datasource.url=jdbc:h2:file:/app/data/mockwebhook
spring.datasource.username=sa
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=false
app.domain=https://your-domain.com
EOF

# Run with custom config
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  -v ./config/application.properties:/app/config/application.properties \
  -e SPRING_CONFIG_LOCATION=/app/config/application.properties \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Custom entrypoint.sh
```bash
# Create custom entrypoint
cat > ./entrypoint.sh << 'EOF'
#!/bin/sh
echo "Running custom entrypoint..."
flyway -url=jdbc:h2:file:/app/data/mockwebhook -user=sa -password=password repair
exec java $JAVA_OPTS -jar ./app.jar
EOF

# Make executable
chmod +x ./entrypoint.sh

# Run with custom entrypoint
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  -v ./entrypoint.sh:/entrypoint.sh \
  ghcr.io/nguyenquy0710/mockapi:latest
```

---

## Volume Mounts

### Data Volume
```bash
# Mount data directory for database persistence
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Logs Volume
```bash
# Mount logs directory
docker run -d -p 8081:8081 \
  -v ./logs:/app/logs \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Combined Volumes
```bash
# Mount both data and logs
docker run -d -p 8081:8081 \
  -v ./data:/app/data \
  -v ./logs:/app/logs \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Docker Compose Volumes
```yaml
volumes:
  - ./data:/app/data
  - ./logs:/app/logs
```

---

## Environment Variables

### Application Configuration
| Variable | Description | Example |
|----------|-------------|---------|
| `TZ` | Timezone | `Asia/Ho_Chi_Minh` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `production` |
| `APP_DOMAIN` | Application domain | `https://your-domain.com` |
| `JAVA_OPTS` | Java options | `-Xmx512m -Xms256m` |

### Database Configuration
| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:h2:file:/app/data/mockwebhook` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `password` |

### Example docker-compose.yml
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
      - SPRING_PROFILES_ACTIVE=production
      - APP_DOMAIN=https://your-domain.com
      - JAVA_OPTS=-Xmx512m -Xms256m -Djava.awt.headless=true
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
```

---

## Building Custom Images

### Build from Source
```bash
# Clone repository
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI

# Build Docker image
docker build -t webhook-mock:latest .

# Tag for registry
docker tag webhook-mock:latest ghcr.io/nguyenquy0710/mockapi:latest

# Push to registry
docker push ghcr.io/nguyenquy0710/mockapi:latest
```

### Build with Custom Tag
```bash
# Build with version tag
docker build -t webhook-mock:v1.0.0 .

# Build with multiple tags
docker build -t webhook-mock:latest -t webhook-mock:v1.0.0 .
```

### Build with Build Arguments
```bash
# Build with custom Java options
docker build \
  --build-arg JAVA_OPTS="-Xmx1024m -Xms512m" \
  -t webhook-mock:custom .

# Build with custom timezone
docker build \
  --build-arg TZ=UTC \
  -t webhook-mock:utc .
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
docker exec mockapi ls -la /app/data/

# Repair database
docker exec mockapi ./repair_database.sh

# Or delete data directory (fresh start)
docker run --rm -v mockapi_data:/data ubuntu rm -rf /data/*
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8081

# Change port in docker-compose.yml
ports:
  - "8082:8081"
```

### Permission Denied
```bash
# Fix permissions
chmod -R 755 ./data
chmod -R 755 ./logs

# Or run as root
docker run -u root ...
```

### Memory Issues
```bash
# Increase memory limit
docker run -d -p 8081:8081 \
  --memory=1g \
  --memory-swap=1g \
  ghcr.io/nguyenquy0710/mockapi:latest
```

### Network Issues
```bash
# Use host network
docker run -d -p 8081:8081 \
  --network=host \
  ghcr.io/nguyenquy0710/mockapi:latest

# Or create custom network
docker network create webhook-network
docker run -d --network=webhook-network ...
```

---

## Best Practices

### Production Best Practices
- Use non-root user
- Set resource limits
- Enable health checks
- Use volume mounts for data
- Configure proper timezone
- Set up monitoring

### Security Best Practices
- Never commit secrets
- Use environment variables for sensitive data
- Enable HTTPS with reverse proxy
- Set up firewall rules
- Regular security scans
- Update base images

### Performance Best Practices
- Use multi-stage builds
- Optimize layer caching
- Use minimal base images
- Configure JVM properly
- Use volume mounts for I/O
- Set up connection pooling

---

## Monitoring

### Health Check
```bash
# Check container health
docker ps

# Check application health
curl http://localhost:8081/actuator/health
```

### Logs
```bash
# View container logs
docker logs -f mockapi

# Follow logs
docker-compose logs -f
```

### Metrics
```bash
# View container stats
docker stats mockapi

# View resource usage
docker system df
```

---

## Next Steps

- [ ] [Deployment Guide](./DEPLOYMENT.md) - Full deployment guide
- [ ] [User Guide](./USER-GUIDE.md) - Using WebHookMock features
- [ ] [Architecture](./ARCHITECTURE.md) - System design
