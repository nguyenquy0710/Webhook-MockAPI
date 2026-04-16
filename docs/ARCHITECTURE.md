# Architecture - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [Architecture Pattern](#architecture-pattern)
- [Layer Structure](#layer-structure)
- [Core Components](#core-components)
- [Database Schema](#database-schema)
- [API Design](#api-design)
- [Security Architecture](#security-architecture)
- [Technology Stack](#technology-stack)

---

## Overview

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

---

## Architecture Pattern

### Clean Architecture Principles
- **Dependency Rule**: Inner layers don't depend on outer layers
- **Separation of Concerns**: Each layer has a specific responsibility
- **Testability**: Business logic is independent of frameworks
- **Flexibility**: Easy to swap implementations (e.g., database, UI)

### Layer Responsibilities

| Layer | Responsibility | Key Components |
|-------|---------------|----------------|
| **Presentation** | Handle HTTP requests/responses | Controllers, DTOs, Views |
| **Business** | Implement business logic | Services, Use Cases |
| **Data** | Data access and persistence | Repositories, Entities, Migrations |

---

## Layer Structure

### Package Organization
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

---

## Core Components

### 1. User Management

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
- `registerUser()` - Create new user with validation
- `createUserByAdmin()` - Admin creates user
- `findByUsername()` - Retrieve user by username
- `toggleUserRole()` - Toggle between USER/ADMIN
- `toggleUserStatus()` - Enable/disable account
- `changePassword()` - Update password with verification

**Business Rules**:
- Username uniqueness enforced
- Email uniqueness enforced
- Password minimum 6 characters
- At least one admin must exist
- Last admin cannot be disabled/deleted

---

### 2. API Configuration

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
- `saveApiConfig()` - Create/update configuration
- `getApiConfigs()` - List user's configurations
- `findApiConfig()` - Match request to configuration
- `processWebhook()` - Handle webhook request with delay
- `buildResponse()` - Build response with template variables

**Template Variables**:
- `{{headers.*}}` - Request headers
- `{{params.*}}` - Query parameters
- `{{method}}` - HTTP method
- `{{path}}` - Request path
- `{{status}}` - Response status
- `{{remoteAddr}}` - Client IP
- `{{cookies.*}}` - Cookie values

---

### 3. Request Logging

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
- `logRequest()` - Create log entry
- `getRequestLogs()` - Paginated log retrieval
- `countRequestLogs()` - Get log count
- `deleteAllRequestLogs()` - Clear logs
- `exportToExcel()` - Export to Excel file

**Curl Generation**:
```bash
curl -X POST -H 'Content-Type: application/json' -d '{"key":"value"}' 'https://domain/api/@username/path'
```

---

### 4. Real-time Updates

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
- `sendRequestUpdate()` - Broadcast request count

**Message Format**:
```json
{
  "type": "REQUEST_UPDATE",
  "count": 100,
  "timestamp": 1681622400000
}
```

---

## Database Schema

### Tables

#### users
Stores user account information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | User ID |
| username | VARCHAR(255) | NOT NULL, UNIQUE | Username (3-20 chars) |
| password | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email address |
| role | VARCHAR(50) | DEFAULT 'USER' | Role (USER/ADMIN) |
| enabled | BOOLEAN | DEFAULT TRUE | Account enabled status |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

#### api_configs
Stores webhook endpoint configurations.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Configuration ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | Owner user ID |
| path | VARCHAR(255) | NOT NULL | Endpoint path |
| method | VARCHAR(10) | NOT NULL | HTTP method |
| content_type | VARCHAR(100) | | Response content type |
| status_code | INTEGER | | HTTP status code (100-599) |
| response_body | CLOB | | Response body template |
| response_headers | CLOB | | Response headers (JSON format) |
| delay_ms | INTEGER | DEFAULT 0 | Response delay in milliseconds |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Update timestamp |

#### request_logs
Stores logs of all incoming webhook requests.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Log ID |
| user_id | BIGINT | NOT NULL, FOREIGN KEY | Owner user ID |
| method | VARCHAR(10) | NOT NULL | HTTP method |
| path | VARCHAR(255) | NOT NULL | Request path |
| source_ip | VARCHAR(45) | | Client IP address |
| query_params | CLOB | | Query parameters string |
| request_headers | CLOB | | Request headers (JSON format) |
| request_body | CLOB | | Request body |
| timestamp | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Request timestamp |
| response_status | INTEGER | | Response status code |
| response_body | CLOB | | Processed response body |
| curl | CLOB | | Generated curl command |

---

## API Design

### REST API Endpoints

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

---

## Security Architecture

### Authentication
- **Method**: Form-based with Spring Security
- **Password Hashing**: BCrypt (cost factor 12)
- **Session Management**: Secure cookies with HttpOnly flag
- **CSRF**: Enabled for forms, disabled for webhook APIs

### Authorization
- **Role-Based Access Control**: USER/ADMIN roles
- **Data Isolation**: Users can only access their own data
- **Ownership Verification**: All write operations verify ownership

### Input Validation
- **Username**: 3-20 chars, alphanumeric + underscore/hyphen
- **Email**: RFC 5322 compliant
- **Password**: Minimum 6 characters
- **HTTP Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD
- **Status Code**: 100-599
- **Delay**: 0-60000ms

---

## Technology Stack

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
| Migration | Flyway | 8.0+ |

---

## Data Flow

### Webhook Request Flow
```
1. HTTP Request → ApiMockController
2. Extract username and path
3. Validate user exists
4. Find matching API configuration
5. Apply configured delay (if any)
6. Process template variables in response
7. Return configured response
8. Log request asynchronously
9. Broadcast WebSocket update
```

### User Registration Flow
```
1. POST /register → AuthController
2. Validate input data
3. Check username uniqueness
4. Check email uniqueness
5. Hash password with BCrypt
6. Save user to database
7. Redirect to login page
```

---

## Error Handling

### Validation Errors
- Return to form with error messages
- Highlight invalid fields
- Display error summary

### Authorization Errors
- 403 Forbidden for unauthorized access
- Redirect to login for unauthenticated

### Business Logic Errors
- Return user-friendly error messages
- Log detailed errors for debugging

### System Errors
- 500 Internal Server Error
- Log stack trace
- Display generic error message

---

## Testing Strategy

### Unit Tests
- **Location**: `src/test/java/vn/autobot/webhook/service/`
- **Coverage**: Service layer methods
- **Framework**: JUnit 5 + Mockito

### Integration Tests
- **Location**: `src/test/java/vn/autobot/webhook/controller/`
- **Coverage**: Controller endpoints
- **Framework**: Spring Boot Test

### Test Commands
```bash
./mvnw test                          # Run all tests
./mvnw test -Dtest=ClassName#methodName  # Run single test
./mvnw clean package                 # Build and test
```

---

## Monitoring and Logging

### Application Logging
- **Framework**: SLF4J + Logback
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Log Format**: Timestamp, level, class, message

### Health Check
- **Endpoint**: `/actuator/health` (requires Spring Boot Actuator)
- **Status**: UP/DOWN
- **Details**: Database connection, disk space

### Metrics
- Request counts
- Response times
- Database query times
- WebSocket connections
