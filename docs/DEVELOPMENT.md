# Development Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Setting Up Development Environment](#setting-up-development-environment)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Code Style and Formatting](#code-style-and-formatting)
- [Database Migrations](#database-migrations)
- [Debugging](#debugging)
- [Common Tasks](#common-tasks)

---

## Setting Up Development Environment

### Prerequisites
- Java 17 or higher
- Maven 3.8.8+
- Git
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

### Clone Repository
```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
```

### Verify Java Version
```bash
java -version
# Should show: openjdk version "17.x.x"
```

### Verify Maven Version
```bash
mvn --version
# Should show: Apache Maven 3.8.8+
```

---

## Running the Application

### Using Maven Wrapper (Recommended)
```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### Using Maven Directly
```bash
mvn spring-boot:run
```

### Running as JAR
```bash
# Build JAR first
./mvnw clean package

# Run JAR
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar
```

### Access Application
- **Main URL**: http://localhost:8081
- **H2 Console**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:file:./data/mockwebhook`
  - Username: `sa`
  - Password: `password`

---

## Running Tests

### Run All Tests
```bash
./mvnw test
```

### Run Single Test Class
```bash
./mvnw test -Dtest=UserServiceTest
```

### Run Single Test Method
```bash
./mvnw test -Dtest=UserServiceTest#testRegisterUser
```

### Run Tests with Coverage
```bash
./mvnw clean test jacoco:report
```

### Test Commands Reference
| Command | Description |
|---------|-------------|
| `./mvnw test` | Run all tests |
| `./mvnw test -Dtest=ClassName` | Run specific test class |
| `./mvnw test -Dtest=ClassName#methodName` | Run specific test method |
| `./mvnw clean package` | Build and test |
| `./mvnw clean test jacoco:report` | Run tests with coverage report |

---

## Code Style and Formatting

### Java Code Style
- Follow **Google Java Style Guide**
- Use **4 spaces** for indentation
- Use **camelCase** for methods and variables
- Use **PascalCase** for classes
- Use **UPPER_SNAKE_CASE** for constants

### IDE Configuration

#### IntelliJ IDEA
1. Import project as Maven project
2. Settings → Editor → Code Style → Java
3. Set tab size to 4
4. Use spaces instead of tabs

#### VS Code
1. Install Java Extension Pack
2. Install Lombok plugin
3. Settings → Java → Editor → Format
4. Set indent size to 4

### Lombok Configuration
```java
// Enable Lombok in IDE
// IntelliJ: Settings → Plugins → Lombok
// VS Code: Install Lombok plugin
```

### Formatting Commands
```bash
# Check code style (with Spotless if configured)
./mvnw spotless:check

# Format code (with Spotless if configured)
./mvnw spotless:apply
```

---

## Database Migrations

### Flyway Configuration
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
```

### Migration Files Location
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_delay_ms_column.sql
└── V3__Add_delay_ms_column.sql
```

### Create New Migration
1. Create SQL file in `src/main/resources/db/migration/`
2. Name format: `V{n}__{description}.sql`
3. Example: `V4__Add_retry_configuration.sql`

```sql
-- Add new column to api_configs table
ALTER TABLE api_configs ADD COLUMN retry_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE api_configs ADD COLUMN max_retries INTEGER DEFAULT 3;
```

### Run Migration Manually
```bash
# Run migration
./mvnw flyway:migrate

# Repair migration (if needed)
./mvnw flyway:repair

# Validate migration
./mvnw flyway:validate
```

### Migration Scripts
```bash
# Validate migration
./validate_migration.sh

# Repair database
./repair_database.sh
```

---

## Debugging

### Enable Debug Logging
```bash
# Linux/macOS
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.org.springframework=DEBUG"

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-Dlogging.level.org.springframework=DEBUG"
```

### Remote Debugging
```bash
# Linux/macOS
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Windows
mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Then attach your IDE debugger to port 5005.

### H2 Console
```properties
# Enable H2 console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Access at: http://localhost:8081/h2-console

---

## Common Tasks

### Add New Entity
1. Create entity class in `src/main/java/vn/autobot/webhook/model/`
2. Create repository interface in `src/main/java/vn/autobot/webhook/repository/`
3. Create service methods in `src/main/java/vn/autobot/webhook/service/`
4. Create controller endpoints in `src/main/java/vn/autobot/webhook/controller/`
5. Create DTOs in `src/main/java/vn/autobot/webhook/dto/`
6. Create Thymeleaf templates in `src/main/resources/templates/`

### Add New API Endpoint
1. Create controller method in appropriate controller
2. Add service method in service class
3. Add repository method if needed
4. Create DTO if needed
5. Add security configuration if needed

### Add New Template Variable
1. Update `RequestUtils.extractRequestContext()` method
2. Add variable to `request-vars.html` fragment
3. Update documentation

### Export API Documentation
```bash
# Generate Swagger JSON
# Access at: http://localhost:8081/swagger.json
```

---

## Troubleshooting

### Lombok Not Working
```bash
# IntelliJ: Settings → Plugins → Lombok → Enable
# VS Code: Install Lombok plugin
```

### Database Issues
```bash
# Repair database
./repair_database.sh

# Or delete data directory
rm -rf ./data/
```

### Port Already in Use
```bash
# Change port in application.properties
server.port=8082
```

### Test Failures
```bash
# Clean and rebuild
./mvnw clean package

# Run tests with verbose output
./mvnw test -X
```

---

## Best Practices

### Code Organization
- Keep controllers thin (business logic in services)
- Use DTOs for API requests/responses
- Validate inputs in controllers
- Handle exceptions in services
- Use transactions for database operations

### Testing
- Write unit tests for service methods
- Write integration tests for controllers
- Test edge cases and error conditions
- Aim for > 80% code coverage

### Security
- Never commit database files
- Never commit secrets or API keys
- Use BCrypt for password hashing
- Validate all user inputs
- Check ownership before modifications

---

## Next Steps

- [ ] [Architecture](./ARCHITECTURE.md) - Understand system design
- [ ] [Testing Guide](./TESTING.md) - Learn testing strategies
- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy to production
