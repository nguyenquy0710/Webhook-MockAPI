# API Specification
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Overview

### 1.1 Base URL
- **Development**: `http://localhost:8081`
- **Production**: `https://domain`

### 1.2 Authentication
- **Method**: Session-based (JSESSIONID cookie)
- **CSRF**: Disabled for `/api/**` endpoints

### 1.3 Content Types
- **Request**: `application/json` (for REST APIs)
- **Response**: `application/json` (for REST APIs)

---

## 2. Authentication API

### 2.1 Login

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

**Notes**: Form-based authentication, uses JSESSIONID cookie

---

### 2.2 Register

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

### 2.3 Logout

**Endpoint**: `GET /logout`

**Response**:
- **302 Found**: Redirects to `/login?logout`
- **Session**: Invalidated, JSESSIONID cookie deleted

---

### 2.4 Change Password

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

## 3. API Configuration API

### 3.1 List API Configurations

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

### 3.2 Create API Configuration

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

### 3.3 Update API Configuration

**Endpoint**: `PUT /api/configs`

**Request Body**: Same as create (includes ID)

**Response**: Updated configuration

**Status Codes**:
- **200 OK**: Configuration updated
- **400 Bad Request**: Validation errors
- **403 Forbidden**: Not authenticated or doesn't own configuration
- **404 Not Found**: Configuration not found

---

### 3.4 Delete API Configuration

**Endpoint**: `DELETE /api/configs/{id}`

**Response**:
- **204 No Content**: Configuration deleted
- **403 Forbidden**: Not authenticated or doesn't own configuration
- **404 Not Found**: Configuration not found

---

### 3.5 Backup API Configurations

**Endpoint**: `POST /api/configs/backup/@{username}`

**Response**:
- **Content-Type**: `application/json`
- **Content-Disposition**: `attachment; filename="api-configs-backup.json"`
- **Body**: JSON array of all configurations

**Status Codes**:
- **200 OK**: Backup generated
- **403 Forbidden**: Not authenticated or doesn't own configurations

---

### 3.6 Restore API Configurations

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

## 4. Webhook API

### 4.1 Webhook Endpoint

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

## 5. Request Logs API

### 5.1 List Request Logs

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

### 5.2 Get Request Log Count

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

### 5.3 Delete All Request Logs

**Endpoint**: `DELETE /api/logs/@{username}`

**Response**:
- **204 No Content**: All logs deleted
- **403 Forbidden**: Not authenticated or doesn't own logs

---

### 5.4 Export Request Logs to Excel

**Endpoint**: `GET /api/logs/@{username}/export`

**Response**:
- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition**: `attachment; filename="RequestLogs_{username}_{timestamp}.xlsx"`
- **Body**: Excel file with columns: ID, Method, Path, Source IP, Query Params, Request Headers, Request Body, Timestamp, Response Status, Response Body, Curl

**Status Codes**:
- **200 OK**: Excel file generated
- **403 Forbidden**: Not authenticated or doesn't own logs

---

## 6. Admin API

### 6.1 List All Users

**Endpoint**: `GET /admin/users`

**Response**: HTML page with user table

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated or not ADMIN role

---

### 6.2 Create User (Admin)

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

### 6.3 Toggle User Role

**Endpoint**: `POST /admin/users/{id}/toggle-role`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot demote last admin user

---

### 6.4 Toggle User Status

**Endpoint**: `POST /admin/users/{id}/toggle-status`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot disable last admin user

---

### 6.5 Delete User

**Endpoint**: `POST /admin/users/{id}/delete`

**Response**:
- **302 Found**: Redirects to `/admin/users`
- **403 Forbidden**: Not authenticated or not ADMIN role
- **404 Not Found**: User not found

**Business Rules**:
- Cannot delete last admin user

---

### 6.6 View User Logs (Admin)

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

### 6.7 Export User Logs (Admin)

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

## 7. API Documentation API

### 7.1 Swagger UI

**Endpoint**: `GET /swagger-ui`

**Response**: Interactive API documentation

**Status Codes**:
- **200 OK**: Success

---

### 7.2 Per-User API Specs

**Endpoint**: `GET /swagger/@{username}`

**Response**: User-specific API documentation

**Status Codes**:
- **200 OK**: Success
- **403 Forbidden**: Not authenticated

---

### 7.3 Raw Swagger JSON

**Endpoint**: `GET /swagger.json`

**Response**:
- **Content-Type**: `application/json`
- **Body**: OpenAPI 3.0 specification

**Status Codes**:
- **200 OK**: Success

---

## 8. WebSocket API

### 8.1 WebSocket Endpoint

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

## 9. Error Responses

### 9.1 Validation Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/configs"
}
```

### 9.2 Authorization Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/configs"
}
```

### 9.3 Not Found Error
```json
{
  "timestamp": "2026-04-16T08:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found",
  "path": "/api/configs/1"
}
```

### 9.4 Server Error
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

## 10. Rate Limiting

**Current Implementation**: None (planned for Phase 3)

**Future Implementation**:
- Per-user rate limiting
- Configurable limits per endpoint
- Exponential backoff for repeated violations

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
