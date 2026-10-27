# WebHookMock - Documentation

A powerful and user-friendly webhook testing tool that allows developers to create, manage, and test webhook APIs with ease.

## 📚 Documentation

### For Users
- **[Installation Guide](./INSTALLATION.md)** - How to install and set up WebHookMock
- **[User Guide](./USER-GUIDE.md)** - Complete guide for using WebHookMock features
- **[API Documentation](./API-DOCUMENTATION.md)** - REST API endpoints and usage

### For Developers
- **[Architecture](./ARCHITECTURE.md)** - System architecture and design patterns
- **[Development Guide](./DEVELOPMENT.md)** - Setting up development environment
- **[Testing Guide](./TESTING.md)** - Running and writing tests
- **[Database Migration](./DATABASE-MIGRATION.md)** - Database schema and migrations

### For DevOps
- **[Deployment Guide](./DEPLOYMENT.md)** - Deploying to production
- **[Docker Deployment](./DOCKER-DEPLOYMENT.md)** - Running with Docker
- **[CI/CD Pipeline](./CI-CD.md)** - Continuous integration and deployment

### Additional Resources
- **[Changelog](../CHANGELOG.md)** - Version history and release notes
- **[Security](../SECURITY.md)** - Security best practices
- **[Contributing](../CONTRIBUTING.md)** - How to contribute

## 🚀 Quick Start

### Docker (Recommended)
```bash
docker pull ghcr.io/nguyenquy0710/mockapi:latest
docker run -d -p 8081:8081 -v ./data:/app/data ghcr.io/nguyenquy0710/mockapi:latest
```

### Local Development
```bash
./mvnw spring-boot:run
```

Access the application at: http://localhost:8081

## 📊 Features

- ✅ Create custom webhook endpoints with configurable responses
- ✅ Real-time request logging and monitoring
- ✅ WebSocket support for live updates
- ✅ Excel export functionality
- ✅ Template variable support in responses
- ✅ Response delay configuration
- ✅ Multi-user support with data isolation
- ✅ Admin user management
- ✅ Swagger API documentation

## 🛠️ Technology Stack

- **Backend**: Spring Boot 2.7.9 (Java 17)
- **Database**: H2 (file-based)
- **Frontend**: Thymeleaf, Bootstrap 5
- **Real-time**: WebSocket (STOMP)
- **Build**: Maven
- **Security**: Spring Security (BCrypt)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## 👥 Authors

- **Nguyen Quy** - [GitHub](https://github.com/nguyenquy0710) | [Blog](https://blogs.nhquydev.net)

## 🙏 Acknowledgments

- Spring Boot team for the amazing framework
- All contributors and users of WebHookMock

---

<p style="text-align: center;"> © 2025 WebHookMock. All rights reserved. </p>
