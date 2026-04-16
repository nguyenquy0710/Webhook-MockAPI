# Functional Specification Document (FSD)
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Introduction

### 1.1 Purpose
This document provides detailed functional specifications for the WebHookMock application, describing how the system will behave from a functional perspective.

### 1.2 Scope
This specification covers all functional requirements for:
- User authentication and management
- Webhook endpoint configuration
- Request processing and response simulation
- Request logging and monitoring
- Real-time updates via WebSocket
- Admin functionality
- API documentation

### 1.3 Definitions
- **FSD**: Functional Specification Document
- **API**: Application Programming Interface
- **DTO**: Data Transfer Object
- **WebSocket**: Full-duplex communication channel over a single TCP connection

---

## 2. System Architecture Overview

### 2.1 High-Level Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                        Web Client (Thymeleaf)               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Dashboard   │  │  API Config  │  │  Request Logs│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Controllers (REST + MVC)                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │   │
│  │  │ AuthController│ │ApiConfigCtrl │ │ApiMockCtrl │  │   │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│  ┌─────────────────────────┼──────────────────────────────┐ │
│  │                         ▼                              │ │
│  │              Services (Business Logic)                 │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │ UserService  │  │ApiMockService│ │RequestLogSrv│  │ │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                 │
│  ┌─────────────────────────┼──────────────────────────────┐ │
│  │                         ▼                              │ │
│  │           Repositories (Data Access Layer)             │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │ │
│  │  │ UserRepository│ │ApiConfigRepo │ │RequestLogRep│  │ │
│  │  └──────────────┘  └──────────────┘  └────────────┘  │ │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      H2 Database                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    users     │  │  api_configs │  │ request_logs │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Technology Stack
- **Backend Framework**: Spring Boot 2.7.9
- **Language**: Java 17
- **Database**: H2 (file-based)
- **ORM**: Spring Data JPA (Hibernate)
- **Security**: Spring Security (BCrypt)
- **Frontend**: Thymeleaf, Bootstrap 5
- **Real-time**: WebSocket (STOMP)
- **Build Tool**: Maven
- **API Docs**: Swagger/OpenAPI

---

## 3. Functional Specifications

### 3.1 User Authentication Module

#### 3.1.1 User Registration

**Endpoint**: `POST /register`

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

**Validation Rules**:
- Username: 3-20 characters, alphanumeric + underscore/hyphen only
- Email: Valid email format, must be unique
- Password: Minimum 6 characters
- Password confirmation must match password

**Response**:
- Success: Redirect to `/login?registered`
- Error: Return to registration page with error messages

**Business Logic**:
1. Validate input data
2. Check username uniqueness
3. Check email uniqueness
4. Verify password confirmation
5. Hash password using BCrypt (cost factor 12)
6. Create user with default role "USER"
7. Save to database

#### 3.1.2 User Login

**Endpoint**: `POST /login` (form-based)

**Request Parameters**:
- username: User's username
- password: User's password

**Response**:
- Success: Redirect to `/dashboard`
- Failure: Return to `/login` with error message

**Business Logic**:
1. Load user by username
2. Verify password using BCrypt matcher
3. Check user enabled status
4. Create authentication session
5. Set secure cookie

#### 3.1.3 Password Change

**Endpoint**: `POST /change-password`

**Request Body**:
```json
{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456",
  "confirmNewPassword": "newpass456"
}
```

**Validation Rules**:
- Current password must match stored password
- New password must be at least 6 characters
- New password confirmation must match

**Response**:
- Success: Success message, redirect back to form
- Error: Error message, return to form

**Business Logic**:
1. Load authenticated user
2. Verify current password
3. Validate new password
4. Hash new password
5. Update user record

#### 3.1.4 User Logout

**Endpoint**: `GET /logout`

**Response**: Redirect to `/login?logout`

**Business Logic**:
1. Invalidate HTTP session
2. Delete JSESSIONID cookie
3. Clear security context
4. Redirect to login page

---

### 3.2 Webhook Configuration Module

#### 3.2.1 Create API Configuration

**Endpoint**: `POST /api/configs`

**Request Body**:
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

**Validation Rules**:
- Path: Required, must be unique per user
- Method: Required, one of GET/POST/PUT/DELETE/PATCH/OPTIONS/HEAD
- Content-Type: Required
- Status Code: 100-599
- Delay: 0-60000ms

**Response**:
```json
{
  "id": 1,
  "path": "/webhook/test",
  "method": "POST",
  "statusCode": 200,
  "createdAt": "2026-04-16T08:00:00"
}
```

**Business Logic**:
1. Load authenticated user
2. Validate configuration
3. Check path uniqueness for user
4. Save configuration with timestamps
5. Return created configuration

#### 3.2.2 List API Configurations

**Endpoint**: `GET /api/configs/@{username}`

**Response**:
```json
{
  "draw": 1,
  "recordsTotal": 10,
  "recordsFiltered": 10,
  "data": [
    {
      "id": 1,
      "path": "/webhook/test",
      "method": "POST",
      "contentType": "application/json",
      "statusCode": 200,
      "delayMs": 1000,
      "createdAt": "2026-04-16T08:00:00"
    }
  ]
}
```

**Business Logic**:
1. Verify user ownership
2. Fetch paginated configurations
3. Convert to DTOs
4. Return in DataTables format

#### 3.2.3 Update API Configuration

**Endpoint**: `PUT /api/configs`

**Request Body**: Same as create (includes ID)

**Response**: Updated configuration

**Business Logic**:
1. Load user and configuration
2. Verify ownership
3. Update fields
4. Update timestamp
5. Save to database

#### 3.2.4 Delete API Configuration

**Endpoint**: `DELETE /api/configs/{id}`

**Response**: 204 No Content

**Business Logic**:
1. Load user and configuration
2. Verify ownership
3. Cascade delete related request logs
4. Delete configuration

#### 3.2.5 Backup API Configurations

**Endpoint**: `POST /api/configs/backup/@{username}`

**Response**:
- Content-Type: application/json
- Content-Disposition: attachment; filename="api-configs-backup.json"
- Body: JSON array of all configurations

**Business Logic**:
1. Load user's configurations
2. Convert to DTOs
3. Serialize to JSON
4. Return as downloadable file

#### 3.2.6 Restore API Configurations

**Endpoint**: `POST /api/configs/restore/@{username}`

**Request Body**: JSON array of configurations

**Response**:
```json
{
  "success": true,
  "message": "API configurations restored successfully"
}
```

**Business Logic**:
1. Load user
2. Deserialize JSON array
3. Create new configurations for user
4. Return success/error response

---

### 3.3 Webhook Request Processing Module

#### 3.3.1 Webhook Endpoint

**Endpoint**: `ANY /api/@{username}/{path}`

**Supported Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD

**Request Flow**:
1. Extract username from path
2. Validate user exists
3. Extract path and method from request
4. Find matching API configuration
5. Read request body
6. Process with configured delay (if any)
7. Apply template variables to response
8. Return configured response
9. Log request asynchronously

**Response Processing**:
1. Parse response headers (JSON)
2. Set Content-Type header
3. Set status code
4. Process template variables in response body
5. Return ResponseEntity

**Template Variables Supported**:
- `{{headers.*}}` - Request headers
- `{{params.*}}` - Query parameters
- `{{method}}` - HTTP method
- `{{path}}` - Request path
- `{{status}}` - Response status code
- `{{remoteAddr}}` - Client IP
- `{{cookies.*}}` - Cookie values

**Example Template**:
```json
{
  "message": "Hello {{headers.User-Agent}}",
  "clientIp": "{{remoteAddr}}",
  "timestamp": "{{method}} {{path}}",
  "status": {{status}}
}
```

---

### 3.4 Request Logging Module

#### 3.4.1 Create Request Log

**Trigger**: After webhook response is sent

**Data Collected**:
- HTTP method
- Request path
- Source IP address
- Query parameters
- Request headers (JSON)
- Request body
- Response status code
- Response body
- Timestamp
- Generated curl command

**Business Logic**:
1. Extract request details
2. Build curl command
3. Process response body with templates
4. Serialize headers/params to JSON
5. Save to database
6. Broadcast WebSocket update

#### 3.4.2 List Request Logs

**Endpoint**: `GET /api/logs/@{username}`

**Query Parameters**:
- page: Page number (default: 0)
- size: Page size (default: 10)
- path: Filter by path (optional)

**Response**:
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
      "responseStatus": 200,
      "responseBody": "{\"message\":\"Hello\"}",
      "timestamp": "2026-04-16T08:00:00",
      "curl": "curl -X POST ..."
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

**Business Logic**:
1. Verify user ownership
2. Apply path filter if provided
3. Fetch paginated results
4. Convert to DTOs
5. Sort by timestamp descending

#### 3.4.3 Get Request Log Count

**Endpoint**: `GET /api/logs/@{username}/count`

**Response**:
```json
{
  "count": 100
}
```

**Business Logic**:
1. Verify user ownership
2. Count logs (with optional path filter)
3. Return count

#### 3.4.4 Delete All Request Logs

**Endpoint**: `DELETE /api/logs/@{username}`

**Response**: 204 No Content

**Business Logic**:
1. Verify user ownership
2. Delete all user's logs
3. Broadcast WebSocket update

#### 3.4.5 Export Request Logs to Excel

**Endpoint**: `GET /api/logs/@{username}/export`

**Response**:
- Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Content-Disposition: attachment; filename="RequestLogs_{username}_{timestamp}.xlsx"

**Columns**:
- ID, Method, Path, Source IP, Query Params, Request Headers, Request Body, Timestamp, Response Status, Response Body, Curl

**Business Logic**:
1. Load all user's logs
2. Create Excel workbook using Apache POI
3. Write header row
4. Write data rows
5. Auto-size columns
6. Write to response output stream

---

### 3.5 Real-time Updates Module

#### 3.5.1 WebSocket Configuration

**Endpoint**: `/ws` (STOMP over WebSocket)

**Fallback**: SockJS for older browsers

**Message Broker**: Simple in-memory broker (`/topic`)

**Client Subscription**: `/topic/requests/{username}`

#### 3.5.2 Broadcast Request Update

**Trigger**: After each request log is created

**Message Format**:
```json
{
  "type": "REQUEST_UPDATE",
  "count": 100,
  "timestamp": 1681622400000
}
```

**Business Logic**:
1. Get user by username
2. Count user's request logs
3. Build message
4. Send to `/topic/requests/{username}`

---

### 3.6 Admin Module

#### 3.6.1 List All Users

**Endpoint**: `GET /admin/users`

**Response**: HTML page with user table

**Columns**:
- Username, Email, Role, Status, Created At, Actions

**Actions**:
- Edit role (toggle USER/ADMIN)
- Enable/Disable
- Delete

**Business Logic**:
1. Verify admin role
2. Load all users
3. Render template

#### 3.6.2 Create User (Admin)

**Endpoint**: `POST /admin/users/create`

**Request Body**:
```json
{
  "username": "newuser",
  "email": "new@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "role": "USER",
  "enabled": true
}
```

**Validation**:
- Username uniqueness
- Email uniqueness
- Password match
- At least one admin must exist

**Response**:
- Success: Redirect to `/admin/users`
- Error: Return to form with messages

#### 3.6.3 Toggle User Role

**Endpoint**: `POST /admin/users/{id}/toggle-role`

**Business Logic**:
1. Verify admin role
2. Load user
3. Check if last admin (cannot demote)
4. Toggle role
5. Save user

#### 3.6.4 Toggle User Status

**Endpoint**: `POST /admin/users/{id}/toggle-status`

**Business Logic**:
1. Verify admin role
2. Load user
3. Check if last admin (cannot disable)
4. Toggle enabled status
5. Save user

#### 3.6.5 Delete User

**Endpoint**: `POST /admin/users/{id}/delete`

**Business Logic**:
1. Verify admin role
2. Load user
3. Check if last admin (cannot delete)
4. Delete user (cascades to configs/logs)
5. Redirect with success message

#### 3.6.6 View Any User's Logs

**Endpoint**: `GET /admin/user/@{username}/logs`

**Query Parameters**:
- page: Page number
- size: Page size
- path: Path filter

**Response**: HTML page with logs table (same as user view)

**Business Logic**:
1. Verify admin role
2. Load target user
3. Fetch paginated logs
4. Render template with admin context

#### 3.6.7 Export Any User's Logs

**Endpoint**: `GET /admin/user/@{username}/export`

**Response**: Excel file (same as user export)

**Business Logic**:
1. Verify admin role
2. Load target user
3. Export to Excel
4. Write to response

---

### 3.7 API Documentation Module

#### 3.7.1 Swagger UI

**Endpoint**: `/swagger-ui`

**Response**: Interactive API documentation

**Features**:
- View all REST endpoints
- Test endpoints directly
- View request/response schemas

#### 3.7.2 Per-User API Specs

**Endpoint**: `/swagger/@{username}`

**Response**: User-specific API documentation

**Business Logic**:
1. Verify user ownership
2. Generate user-specific documentation
3. Render Swagger UI

#### 3.7.3 Raw Swagger JSON

**Endpoint**: `/swagger.json`

**Response**: OpenAPI 3.0 specification JSON

**Business Logic**:
1. Generate OpenAPI spec
2. Return as JSON

---

## 4. Data Models

### 4.1 User Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto | User ID |
| username | String | Unique, Not Null | Username (3-20 chars) |
| password | String | Not Null | BCrypt hashed password |
| email | String | Unique, Not Null | Email address |
| role | String | Default: "USER" | Role (USER/ADMIN) |
| enabled | Boolean | Default: true | Account enabled |
| createdAt | LocalDateTime | Auto | Creation timestamp |

### 4.2 ApiConfig Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto | Configuration ID |
| user | User | FK, Not Null | Owner user |
| path | String | Not Null | Endpoint path |
| method | String | Not Null | HTTP method |
| contentType | String | Not Null | Response content type |
| statusCode | Integer | 100-599 | HTTP status code |
| responseBody | String | CLOB | Response body template |
| responseHeaders | String | CLOB | Response headers (JSON) |
| delayMs | Integer | 0-60000 | Response delay (ms) |
| createdAt | LocalDateTime | Auto | Creation timestamp |
| updatedAt | LocalDateTime | Auto | Update timestamp |

### 4.3 RequestLog Entity

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, Auto | Log ID |
| user | User | FK, Not Null | Owner user |
| method | String | Not Null | HTTP method |
| path | String | Not Null | Request path |
| sourceIp | String | | Client IP address |
| queryParams | String | CLOB | Query parameters |
| requestHeaders | String | CLOB | Request headers (JSON) |
| requestBody | String | CLOB | Request body |
| timestamp | LocalDateTime | Auto | Request timestamp |
| responseStatus | Integer | | Response status code |
| responseBody | String | CLOB | Response body |
| curl | String | CLOB | Generated curl command |

---

## 5. API Endpoints Summary

### 5.1 Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /login | Public | Login page |
| POST | /login | Public | Login action |
| GET | /register | Public | Registration page |
| POST | /register | Public | Registration action |
| GET | /logout | Auth | Logout action |
| GET | /change-password | Auth | Change password page |
| POST | /change-password | Auth | Change password action |

### 5.2 API Configuration
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/configs/@{username} | User | List configurations |
| POST | /api/configs | User | Create configuration |
| PUT | /api/configs | User | Update configuration |
| DELETE | /api/configs/{id} | User | Delete configuration |
| POST | /api/configs/backup/@{username} | User | Backup configurations |
| POST | /api/configs/restore/@{username} | User | Restore configurations |

### 5.3 Webhook Processing
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| ANY | /api/@{username}/** | Public | Webhook endpoint |

### 5.4 Request Logs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/logs/@{username} | User | List logs |
| GET | /api/logs/@{username}/count | User | Get log count |
| DELETE | /api/logs/@{username} | User | Delete all logs |
| GET | /api/logs/@{username}/export | User | Export to Excel |

### 5.5 Admin
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /admin/users | ADMIN | List users |
| GET | /admin/users/create | ADMIN | Create user form |
| POST | /admin/users/create | ADMIN | Create user |
| POST | /admin/users/{id}/delete | ADMIN | Delete user |
| POST | /admin/users/{id}/toggle-role | ADMIN | Toggle role |
| POST | /admin/users/{id}/toggle-status | ADMIN | Toggle status |
| GET | /admin/user/@{username}/logs | ADMIN | View user logs |
| GET | /admin/user/@{username}/export | ADMIN | Export user logs |

### 5.6 API Documentation
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /swagger-ui | Public | Swagger UI |
| GET | /swagger/@{username} | User | User-specific docs |
| GET | /swagger.json | Public | Raw OpenAPI spec |

### 5.7 WebSocket
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| WS | /ws | Public | WebSocket endpoint |

---

## 6. Business Rules

### 6.1 User Management Rules
- Username must be unique
- Email must be unique
- Password minimum 6 characters
- Username 3-20 characters, alphanumeric + underscore/hyphen
- At least one admin must exist
- Last admin cannot be disabled
- Last admin cannot be deleted

### 6.2 Webhook Configuration Rules
- Path must be unique per user
- Delay 0-60000ms
- Status code 100-599
- Response body supports template variables

### 6.3 Request Handling Rules
- Process requests in order
- Apply delay before responding
- Log after response sent
- Generate curl command

### 6.4 Data Retention Rules
- Logs retained 30 days by default
- Users can delete logs anytime
- Admins can view/export any user's logs

---

## 7. Error Handling

### 7.1 Validation Errors
- Return to form with error messages
- Highlight invalid fields
- Display error summary

### 7.2 Authorization Errors
- 403 Forbidden for unauthorized access
- Redirect to login for unauthenticated

### 7.3 Business Logic Errors
- Return user-friendly error messages
- Log detailed errors for debugging

### 7.4 System Errors
- 500 Internal Server Error
- Log stack trace
- Display generic error message

---

## 8. Future Enhancements

### Phase 2
- Webhook retry configuration
- Webhook signature verification
- Webhook request replay
- Advanced filtering and search
- Export to CSV/JSON/PDF

### Phase 3
- Team collaboration features
- Shared webhook configurations
- Webhook analytics and insights
- API usage quotas
- Rate limiting per user

### Phase 4
- Webhook testing automation
- Mock server for multiple endpoints
- Integration with CI/CD pipelines
- Advanced template engine
- Custom domain support

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
