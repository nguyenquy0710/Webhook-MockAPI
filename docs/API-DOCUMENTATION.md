# API Documentation - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [API Configuration Endpoints](#api-configuration-endpoints)
- [Webhook Endpoints](#webhook-endpoints)
- [Request Logs Endpoints](#request-logs-endpoints)
- [Admin Endpoints](#admin-endpoints)
- [WebSocket Endpoints](#websocket-endpoints)
- [Error Responses](#error-responses)
- [Template Variables](#template-variables)

---

## Overview

### Base URL
- **Development**: http://localhost:8081
- **Production**: https://your-domain.com

### Authentication
- **Method**: Session-based (JSESSIONID cookie)
- **CSRF**: Disabled for `/api/**` endpoints

### Content Types
- **Request**: `application/json` (for REST APIs)
- **Response**: `application/json` (for REST APIs)

---

## Authentication

### Login
**Endpoint**: `POST /login`

**Request Body**:
```json
{
  "username": "johndoe",
  "password": "password123"
}
```

**Response**:
- **200 OK**: Login successful, redirects to `/dashboard`
- **401 Unauthorized**: Invalid credentials
- **403 Forbidden**: Account disabled

---

### Register
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
- Username: 3-20 chars, alphanumeric + underscore/hyphen
- Email: Valid RFC 5322 format
- Password: Minimum 6 characters
- Password confirmation must match

**Response**:
- **200 OK**: Registration successful, redirects to `/login?registered`
- **400 Bad Request**: Validation errors
- **409 Conflict**: Username or email already exists

---

### Logout
**Endpoint**: `GET /logout`

**Response**:
- **302 Found**: Redirects to `/login?logout`
- **Session**: Invalidated, JSESSIONID cookie deleted

---

### Change Password
**Endpoint**: `POST /change-password`

**Request Body**:
```json
{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456",
  "confirmNewPassword": "newpass456"
}
```

**Response**:
- **200 OK**: Password changed successfully
- **400 Bad Request**: Validation errors or current password incorrect
- **401 Unauthorized**: Not authenticated

---

## API Configuration Endpoints

### List API Configurations
**Endpoint**: `GET /api/configs/@{username}`

**Query Parameters**:
- `draw`: DataTables draw counter
- `start`: Pagination start index
- `length`: Page size
- `search[value]`: Search query (optional)

**Headers**:
- `Authorization`: Session cookie (JSESSIONID)

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

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: User doesn't own the configurations

---

### Create API Configuration
**Endpoint**: `POST /api/configs`

**Request Body**:
```json
{
  "path": "/webhook/test",
  "method": "POST",
  "contentType": "application/json",
  "statusCode": 200,
  "responseBody": "{\"message\": \"Hello\"}",
  "responseHeaders": "{\"X-Custom-Header\": \"value\"}",
  "delayMs": 1000
}
```

**Validation Rules**:
- Path: Required, unique per user
- Method: Required (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD)
- Content-Type: Required
- Status Code: 100-599
- Delay: 0-60000ms

**Response**:
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

**Status Codes**:
- **200 OK**: Configuration created
- **400 Bad Request**: Validation errors
- **403 Forbidden**: Not authenticated
- **409 Conflict**: Path already exists for user

---

### Update API Configuration
**Endpoint**: `PUT /api/configs`

**Request Body**: Same as create (includes ID)

**Response**: Updated configuration

**Status Codes**:
- **200 OK**: Configuration updated
- **400 Bad Request**: Validation errors
- **403 Forbidden**: Not authenticated or doesn't own configuration
- **404 Not Found**: Configuration not found

---

### Delete API Configuration
**Endpoint**: `DELETE /api/configs/{id}`

**Response**:
- **204 No Content**: Configuration deleted
- **403 Forbidden**: Not authenticated or doesn't own configuration
- **404 Not Found**: Configuration not found

---

### Backup API Configurations
**Endpoint**: `POST /api/configs/backup/@{username}`

**Response**:
- **Content-Type**: `application/json`
- **Content-Disposition**: `attachment; filename="api-configs-backup.json"`
- **Body**: JSON array of all configurations

**Status Codes**:
- **200 OK**: Backup generated
- **403 Forbidden**: Not authenticated or doesn't own configurations

---

### Restore API Configurations
**Endpoint**: `POST /api/configs/restore/@{username}`

**Request Body**: JSON array of configurations

**Response**:
```json
{
  "success": true,
  "message": "API configurations restored successfully"
}
```

**Status Codes**:
- **200 OK**: Configurations restored
- **400 Bad Request**: Invalid JSON or validation errors
- **403 Forbidden**: Not authenticated or doesn't own configurations

---

## Webhook Endpoints

### Webhook Endpoint
**Endpoint**: `ANY /api/@{username}/{path}`

**Supported Methods**: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD

**Path Parameters**:
- `username`: User's username
- `path`: Webhook path (can include multiple segments)

**Request Processing**:
1. Extract username from path
2. Validate user exists
3. Match request to API configuration
4. Apply configured delay (if any)
5. Process template variables in response
6. Return configured response
7. Log request asynchronously

**Response**:
- **200 OK**: Success (with configured response)
- **404 Not Found**: User not found or no matching configuration

**Template Variables**:
- `{{headers.*}}` - Request headers (e.g., `{{headers.User-Agent}}`)
- `{{params.*}}` - Query parameters (e.g., `{{params.id}}`)
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

## Request Logs Endpoints

### List Request Logs
**Endpoint**: `GET /api/logs/@{username}`

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

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

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or doesn't own logs

---

### Get Request Log Count
**Endpoint**: `GET /api/logs/@{username}/count`

**Response**:
```json
{
  "count": 100
}
```

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or doesn't own logs

---

### Delete All Request Logs
**Endpoint**: `DELETE /api/logs/@{username}`

**Response**:
- **204 No Content**: All logs deleted
- **403 Forbidden**: Not authenticated or doesn't own logs

---

### Export Request Logs to Excel
**Endpoint**: `GET /api/logs/@{username}/export`

**Response**:
- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition**: `attachment; filename="RequestLogs_{username}_{timestamp}.xlsx"`
- **Body**: Excel file with columns: ID, Method, Path, Source IP, Query Params, Request Headers, Request Body, Timestamp, Response Status, Response Body, Curl

**Status Codes**:
- **200 OK**: Excel file generated
- **403 Forbidden**: Not authenticated or doesn't own logs

---

## Admin Endpoints

### List All Users
**Endpoint**: `GET /admin/users`

**Response**: HTML page with user table

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or not ADMIN role

---

### Create User (Admin)
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

**Response**:
- **200 OK**: User created, redirects to `/admin/users`
- **400 Bad Request**: Validation errors
- **403 Forbidden**: Not authenticated or not ADMIN role
- **409 Conflict**: Username or email already exists

---

### Toggle User Role
**Endpoint**: `POST /admin/users/{id}/toggle-role`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot demote last admin user

---

### Toggle User Status
**Endpoint**: `POST /admin/users/{id}/toggle-status`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot disable last admin user

---

### Delete User
**Endpoint**: `POST /admin/users/{id}/delete`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot delete last admin user

---

### View User Logs (Admin)
**Endpoint**: `GET /admin/user/@{username}/logs`

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `path`: Path filter (optional)

**Response**: HTML page with logs table

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

---

### Export User Logs (Admin)
**Endpoint**: `GET /admin/user/@{username}/export`

**Response**:
- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition**: `attachment; filename="RequestLogs_{username}_{timestamp}.xlsx"`
- **Body**: Excel file

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

---

## WebSocket Endpoints

### WebSocket Endpoint
**Endpoint**: `ws://localhost:8081/ws`

**Protocol**: STOMP over WebSocket

**Fallback**: SockJS for older browsers

**Client Subscription**: `/topic/requests/{username}`

**Message Format**:
```json
{
  "type": "REQUEST_UPDATE",
  "count": 100,
  "timestamp": 1681622400000
}
```

**Usage Example**:
```javascript
const stompClient = new StompJs.Client({
  brokerURL: 'ws://localhost:8081/ws'
});

stompClient.onConnect = () => {
  stompClient.subscribe('/topic/requests/johndoe', (frame) => {
    const message = JSON.parse(frame.body);
    console.log('Request count:', message.count);
  });
};

stompClient.activate();
```

---

## Error Responses

### Validation Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/configs"
}
```

### Authorization Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/configs"
}
```

### Not Found Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found",
  "path": "/api/configs/1"
}
```

### Server Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred",
  "path": "/api/configs"
}
```

---

## Template Variables

### Supported Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `{{headers.*}}` | Request headers | `{{headers.User-Agent}}` |
| `{{params.*}}` | Query parameters | `{{params.id}}` |
| `{{method}}` | HTTP method | `{{method}}` |
| `{{path}}` | Request path | `{{path}}` |
| `{{status}}` | Response status | `{{status}}` |
| `{{remoteAddr}}` | Client IP | `{{remoteAddr}}` |
| `{{cookies.*}}` | Cookie values | `{{cookies.session}}` |

### Context Variables

| Variable | Description |
|----------|-------------|
| `status` | Response status code |
| `method` | HTTP method |
| `path` | Request path |
| `remoteAddr` | Client IP address |
| `remoteHost` | Client host name |
| `remotePort` | Client port |
| `localAddr` | Local IP address |
| `localName` | Local host name |
| `localPort` | Local port |
| `url` | Full request URL |
| `uri` | Request URI |
| `protocol` | Protocol |
| `scheme` | URL scheme |
| `serverName` | Server name |
| `serverPort` | Server port |
| `contextPath` | Context path |
| `servletPath` | Servlet path |
| `characterEncoding` | Character encoding |
| `contentType` | Content type |

### Example Templates

#### JSON Response
```json
{
  "message": "Hello {{headers.User-Agent}}",
  "clientIp": "{{remoteAddr}}",
  "timestamp": "{{method}} {{path}}",
  "status": {{status}},
  "requestId": "{{headers.X-Request-ID}}"
}
```

#### Plain Text Response
```
Request Details:
- Method: {{method}}
- Path: {{path}}
- Client IP: {{remoteAddr}}
- Timestamp: {{timestamp}}
```

#### HTML Response
```html
<!DOCTYPE html>
<html>
<head><title>Webhook Response</title></head>
<body>
  <h1>Webhook Received</h1>
  <p>Client IP: {{remoteAddr}}</p>
  <p>Method: {{method}}</p>
  <p>Path: {{path}}</p>
</body>
</html>
```

---

## API Documentation

### Swagger UI
- **URL**: http://localhost:8081/swagger-ui
- **Features**:
  - View all REST endpoints
  - Test endpoints directly
  - View request/response schemas

### Per-User API Specs
- **URL**: http://localhost:8081/swagger/@{username}
- Shows user-specific API documentation

### Raw Swagger JSON
- **URL**: http://localhost:8081/swagger.json
- OpenAPI 3.0 specification
- Can be used with API testing tools

---

## Rate Limiting

**Current Implementation**: None (planned for Phase 3)

**Future Implementation**:
- Per-user rate limiting
- Configurable limits per endpoint
- Exponential backoff for repeated violations
