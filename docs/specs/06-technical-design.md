# Technical Design Document (TDD)
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Overview

### 1.1 Architecture Pattern
WebHookMock follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Controllers  │  │   Views      │  │   DTOs       │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Business Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Services    │  │  Use Cases   │  │   Domain     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   Infrastructure Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Repositories │  │  Database    │  │   External   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Technology Stack
| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 2.7.9 |
| Language | Java | 17 |
| Database | H2 | 2.1.214 |
| ORM | Hibernate | 5.6.15 |
| Security | Spring Security | 5.7.3 |
| Frontend | Thymeleaf | 3.1.1 |
| Styling | Bootstrap | 5.3.0 |
| Real-time | WebSocket (STOMP) | Spring WebSocket |
| Build | Maven | 3.8.8 |
| API Docs | Swagger | 3.0.0 |

---

## 2. Module Structure

### 2.1 Package Organization
```
vn.autobot.webhook/
├── WebHookMockApplication.java          # Application entry point
├── config/                              # Configuration classes
│   ├── SecurityConfig.java             # Spring Security setup
│   ├── WebSocketConfig.java            # WebSocket configuration
│   ├── WebConfig.java                  # Web MVC configuration
│   ├── AppConfig.java                  # Application properties
│   ├── DatabaseMigrationConfig.java    # Flyway configuration
│   └── UserDetailsServiceImpl.java     # UserDetailsService
├── controller/                          # REST and MVC controllers
│   ├── AuthController.java             # Authentication endpoints
│   ├── ApiConfigController.java        # API configuration REST API
│   ├── ApiMockController.java          # Webhook processing
│   ├── RequestLogController.java       # Request logs REST API
│   ├── AdminController.java            # Admin endpoints
│   ├── WebhookViewController.java      # MVC views
│   └── SwaggerUiController.java        # Swagger UI
├── service/                             # Business logic
│   ├── UserService.java                # User management
│   ├── ApiConfigService.java           # API config business logic
│   ├── ApiMockService.java             # Webhook processing
│   ├── RequestLogService.java          # Request logging
│   ├── WebSocketService.java           # WebSocket updates
│   ├── SwaggerService.java             # API documentation
│   └── DataInitializationService.java  # Initial data setup
├── repository/                          # Data access layer
│   ├── UserRepository.java
│   ├── ApiConfigRepository.java
│   └── RequestLogRepository.java
├── model/                               # Domain entities
│   ├── User.java
│   ├── ApiConfig.java
│   └── RequestLog.java
├── dto/                                 # Data Transfer Objects
│   ├── RegisterRequestDto.java
│   ├── ChangePasswordRequestDto.java
│   ├── CreateUserRequestDto.java
│   ├── ApiConfigDto.java
│   └── RequestLogDto.java
└── utils/                               # Utility classes
    └── RequestUtils.java               # Request processing utilities
```

### 2.2 Layer Responsibilities

#### Presentation Layer
- **Controllers**: Handle HTTP requests/responses
- **DTOs**: Transfer data between layers
- **Views**: Thymeleaf templates for HTML rendering

#### Business Layer
- **Services**: Implement business logic
- **Validation**: Input validation and business rules
- **Authorization**: Ownership verification

#### Data Layer
- **Repositories**: JPA data access
- **Entities**: Database models
- **Migrations**: Flyway schema management

---

## 3. Core Components

### 3.1 User Management

#### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    private Long id;
    private String username;      // Unique, 3-20 chars
    private String password;      // BCrypt hashed
    private String email;         // Unique
    private String role;          // USER/ADMIN
    private Boolean enabled;      // Account status
    private LocalDateTime createdAt;
}
```

#### UserService
**Key Methods**:
- `registerUser()`: Create new user with validation
- `createUserByAdmin()`: Admin creates user
- `findByUsername()`: Retrieve user by username
- `toggleUserRole()`: Toggle between USER/ADMIN
- `toggleUserStatus()`: Enable/disable account
- `changePassword()`: Update password with verification

**Business Rules**:
- Username uniqueness enforced
- Email uniqueness enforced
- Password minimum 6 characters
- At least one admin must exist
- Last admin cannot be disabled/deleted

### 3.2 API Configuration

#### ApiConfig Entity
```java
@Entity
@Table(name = "api_configs")
public class ApiConfig {
    private Long id;
    private User user;            // Owner
    private String path;          // Endpoint path
    private String method;        // HTTP method
    private String contentType;   // Response type
    private Integer statusCode;   // 100-599
    private String responseBody;  // Template with variables
    private String responseHeaders; // JSON format
    private Integer delayMs;      // 0-60000ms
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### ApiMockService
**Key Methods**:
- `saveApiConfig()`: Create/update configuration
- `getApiConfigs()`: List user's configurations
- `findApiConfig()`: Match request to configuration
- `processWebhook()`: Handle webhook request with delay
- `buildResponse()`: Build response with template variables

**Template Variables**:
- `{{headers.*}}` - Request headers
- `{{params.*}}` - Query parameters
- `{{method}}` - HTTP method
- `{{path}}` - Request path
- `{{status}}` - Response status
- `{{remoteAddr}}` - Client IP
- `{{cookies.*}}` - Cookie values

### 3.3 Request Logging

#### RequestLog Entity
```java
@Entity
@Table(name = "request_logs")
public class RequestLog {
    private Long id;
    private User user;
    private String method;
    private String path;
    private String sourceIp;
    private String queryParams;
    private String requestHeaders;  // JSON
    private String requestBody;
    private LocalDateTime timestamp;
    private Integer responseStatus;
    private String responseBody;
    private String curl;            // Generated curl command
}
```

#### RequestLogService
**Key Methods**:
- `logRequest()`: Create log entry
- `getRequestLogs()`: Paginated log retrieval
- `countRequestLogs()`: Get log count
- `deleteAllRequestLogs()`: Clear logs
- `exportToExcel()`: Export to Excel file

**Curl Generation**:
```bash
curl -X POST -H 'Content-Type: application/json' -d '{"key":"value"}' 'https://domain/api/@username/path'
```

### 3.4 Real-time Updates

#### WebSocket Configuration
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // Endpoint: /ws
    // Broker: /topic
    // Client subscription: /topic/requests/{username}
}
```

#### WebSocketService
**Key Methods**:
- `sendRequestUpdate()`: Broadcast request count

**Message Format**:
```json
{
  "type": "REQUEST_UPDATE",
  "count": 100,
  "timestamp": 1681622400000
}
```

---

## 4. API Design

### 4.1 REST API Endpoints

#### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /login | Login page |
| POST | /login | Login action |
| GET | /register | Registration page |
| POST | /register | Register user |
| GET | /logout | Logout |
| GET | /change-password | Password change page |
| POST | /change-password | Change password |

#### API Configuration
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/configs/@{username} | List configurations |
| POST | /api/configs | Create configuration |
| PUT | /api/configs | Update configuration |
| DELETE | /api/configs/{id} | Delete configuration |
| POST | /api/configs/backup/@{username} | Backup configurations |
| POST | /api/configs/restore/@{username} | Restore configurations |

#### Webhook Processing
| Method | Endpoint | Description |
|--------|----------|-------------|
| ANY | /api/@{username}/** | Webhook endpoint |

#### Request Logs
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/logs/@{username} | List logs |
| GET | /api/logs/@{username}/count | Get log count |
| DELETE | /api/logs/@{username} | Delete all logs |
| GET | /api/logs/@{username}/export | Export to Excel |

#### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /admin/users | List users |
| POST | /admin/users/{id}/delete | Delete user |
| POST | /admin/users/{id}/toggle-role | Toggle role |
| POST | /admin/users/{id}/toggle-status | Toggle status |
| GET | /admin/user/@{username}/logs | View user logs |
| GET | /admin/user/@{username}/export | Export user logs |

### 4.2 Request/Response Formats

#### Create API Configuration Request
```json
{
  "path": "/webhook/test",
  "method": "POST",
  "contentType": "application/json",
  "statusCode": 200,
  "responseBody": "{\"message\": \"Hello {{headers.User-Agent}}\"}",
  "responseHeaders": "{\"X-Custom-Header\": \"value\"}",
  "delayMs": 1000
}
```

#### API Configuration Response
```json
{
  "id": 1,
  "path": "/webhook/test",
  "method": "POST",
  "contentType": "application/json",
  "statusCode": 200,
  "delayMs": 1000,
  "createdAt": "2026-04-16T08:00:00"
}
```

#### Request Logs Response
```json
{
  "content": [
    {
      "id": 1,
      "method": "POST",
      "path": "/webhook/test",
      "sourceIp": "192.168.1.1",
      "queryParams": "key=value",
      "requestHeaders": "{\"Content-Type\":\"application/json\"}",
      "requestBody": "{\"data\":\"test\"}",
      "timestamp": "2026-04-16T08:00:00",
      "responseStatus": 200,
      "responseBody": "{\"message\":\"Hello\"}",
      "curl": "curl -X POST ..."
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

---

## 5. Security Design

### 5.1 Authentication
- **Method**: Form-based with Spring Security
- **Password Hashing**: BCrypt (cost factor 12)
- **Session Management**: Secure cookies with HttpOnly flag
- **CSRF**: Enabled for forms, disabled for webhook APIs

### 5.2 Authorization
- **Role-Based Access Control**: USER/ADMIN roles
- **Data Isolation**: Users can only access their own data
- **Ownership Verification**: All write operations verify ownership

### 5.3 Input Validation
- **Username**: 3-20 chars, alphanumeric + underscore/hyphen
- **Email**: RFC 5322 compliant
- **Password**: Minimum 6 characters
- **HTTP Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD
- **Status Code**: 100-599
- **Delay**: 0-60000ms

---

## 6. Database Design

### 6.1 Schema
```sql
-- users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- api_configs table
CREATE TABLE api_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    path VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    content_type VARCHAR(100),
    status_code INTEGER,
    response_body CLOB,
    response_headers CLOB,
    delay_ms INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- request_logs table
CREATE TABLE request_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    source_ip VARCHAR(45),
    query_params CLOB,
    request_headers CLOB,
    request_body CLOB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_status INTEGER,
    response_body CLOB,
    curl CLOB,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 6.2 Indexes
```sql
CREATE INDEX idx_api_configs_user_id ON api_configs(user_id);
CREATE INDEX idx_api_configs_path_method ON api_configs(path, method);
CREATE INDEX idx_request_logs_user_id ON request_logs(user_id);
CREATE INDEX idx_request_logs_timestamp ON request_logs(timestamp);
```

### 6.3 Migrations
- **V1**: Initial schema (users, api_configs, request_logs)
- **V2**: Add delay_ms column to api_configs
- **V3**: Add missing columns and data cleanup

---

## 7. Deployment Architecture

### 7.1 Development
- **Database**: H2 file-based at `./data/mockwebhook`
- **Port**: 8081
- **H2 Console**: `/h2-console`

### 7.2 Production
- **Database**: H2 file-based or external database
- **Port**: 8081 (configurable)
- **SSL**: Nginx reverse proxy with Let's Encrypt

### 7.3 Docker
```yaml
version: '3.8'
services:
  web:
    image: ghcr.io/nguyenquy0710/mockapi:latest
    ports:
      - "8081:8081"
    volumes:
      - ./data:/app/data
```

---

## 8. Error Handling

### 8.1 Validation Errors
- Return to form with error messages
- Highlight invalid fields
- Display error summary

### 8.2 Authorization Errors
- 403 Forbidden for unauthorized access
- Redirect to login for unauthenticated

### 8.3 Business Logic Errors
- Return user-friendly error messages
- Log detailed errors for debugging

### 8.4 System Errors
- 500 Internal Server Error
- Log stack trace
- Display generic error message

---

## 9. Testing Strategy

### 9.1 Unit Tests
- **Location**: `src/test/java/vn/autobot/webhook/service/`
- **Coverage**: Service layer methods
- **Framework**: JUnit 5 + Mockito

### 9.2 Integration Tests
- **Location**: `src/test/java/vn/autobot/webhook/controller/`
- **Coverage**: Controller endpoints
- **Framework**: Spring Boot Test

### 9.3 Test Commands
```bash
./mvnw test                          # Run all tests
./mvnw test -Dtest=ClassName#methodName  # Run single test
./mvnw clean package                 # Build and test
```

---

## 10. Monitoring and Logging

### 10.1 Application Logging
- **Framework**: SLF4J + Logback
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Log Format**: Timestamp, level, class, message

### 10.2 Health Check
- **Endpoint**: `/actuator/health` (requires Spring Boot Actuator)
- **Status**: UP/DOWN
- **Details**: Database connection, disk space

### 10.3 Metrics
- Request counts
- Response times
- Database query times
- WebSocket connections

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
