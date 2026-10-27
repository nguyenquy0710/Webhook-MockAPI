# User Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Getting Started](#getting-started)
- [User Registration](#user-registration)
- [Creating Webhook Endpoints](#creating-webhook-endpoints)
- [Configuring Response](#configuring-response)
- [Using Template Variables](#using-template-variables)
- [Request Logs](#request-logs)
- [Exporting Data](#exporting-data)
- [Admin Features](#admin-features)
- [WebSocket Real-time Updates](#websocket-real-time-updates)
- [API Documentation](#api-documentation)

---

## Getting Started

### Access the Application
- **URL**: http://localhost:8081
- **Default Port**: 8081

### First Time Setup
1. Navigate to http://localhost:8081
2. Click "Register" button
3. Fill in registration form
4. Click "Register"
5. Login with your credentials

---

## User Registration

### Registration Form
| Field | Required | Description |
|-------|----------|-------------|
| Username | Yes | 3-20 characters, alphanumeric + underscore/hyphen |
| Email | Yes | Valid email address |
| Password | Yes | Minimum 6 characters |
| Confirm Password | Yes | Must match password |

### Validation Rules
- Username must be unique
- Email must be unique and valid format
- Password must be at least 6 characters
- Password and confirm password must match

### After Registration
- Redirected to login page
- Can login with credentials
- Redirected to dashboard after login

---

## Creating Webhook Endpoints

### Navigate to Dashboard
1. Login to your account
2. Click "Dashboard" from navigation menu
3. Click "Create Webhook" button

### Webhook Configuration Form

#### Basic Information
| Field | Required | Description |
|-------|----------|-------------|
| Path | Yes | Endpoint path (e.g., `/webhook/test`) |
| Method | Yes | HTTP method (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD) |
| Content-Type | Yes | Response content type (application/json, text/plain, etc.) |

#### Response Configuration
| Field | Required | Description |
|-------|----------|-------------|
| Status Code | Yes | HTTP status code (100-599) |
| Response Body | No | Response body with template variables |
| Response Headers | No | JSON format headers |

#### Advanced Options
| Field | Required | Description |
|-------|----------|-------------|
| Delay (ms) | No | Response delay (0-60000ms) |

### Example Configuration
```json
{
  "path": "/webhook/test",
  "method": "POST",
  "contentType": "application/json",
  "statusCode": 200,
  "responseBody": "{\"message\": \"Hello {{headers.User-Agent}}\", \"clientIp\": \"{{remoteAddr}}\"}",
  "responseHeaders": "{\"X-Custom-Header\": \"value\"}",
  "delayMs": 1000
}
```

### After Creating
- Webhook endpoint created
- Can view in webhook list
- Can test by sending request to endpoint

---

## Configuring Response

### Response Body
- Supports plain text or JSON
- Can use template variables for dynamic content
- Example: `{"message": "Hello {{headers.User-Agent}}"}`

### Response Headers
- JSON format
- Example: `{"X-Custom-Header": "value", "X-Request-ID": "{{headers.X-Request-ID}}"}`

### Status Code
- Range: 100-599
- Common codes: 200, 201, 400, 404, 500

### Response Delay
- Range: 0-60000ms (0-60 seconds)
- Useful for simulating slow APIs
- Default: 0 (no delay)

---

## Using Template Variables

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

## Request Logs

### View Request Logs
1. Navigate to "Request Logs" from dashboard
2. View paginated list of requests
3. Filter by path if needed

### Log Information
Each log entry includes:
- ID
- HTTP Method
- Path
- Source IP
- Query Parameters
- Request Headers
- Request Body
- Timestamp
- Response Status
- Response Body
- Generated curl command

### Log Management
- **Delete All Logs**: Click "Delete All" button
- **Export to Excel**: Click "Export to Excel" button
- **Copy curl**: Click "Copy" button for any request

### Real-time Updates
- Logs update automatically via WebSocket
- No need to refresh page
- See new requests as they arrive

---

## Exporting Data

### Export Request Logs to Excel
1. Navigate to "Request Logs" page
2. Click "Export to Excel" button
3. Excel file downloads automatically
4. File name format: `RequestLogs_{username}_{timestamp}.xlsx`

### Export API Configurations
1. Navigate to "API Configs" page
2. Click "Backup" button
3. JSON file downloads automatically
4. File name format: `api-configs-backup.json`

### Restore API Configurations
1. Navigate to "API Configs" page
2. Click "Restore" button
3. Select JSON backup file
4. Click "Upload"
5. Configurations restored

---

## Admin Features

### Access Admin Panel
1. Login with ADMIN role
2. Navigate to "Admin" from navigation menu
3. Access user management features

### User Management
| Feature | Description |
|---------|-------------|
| View Users | See all users in system |
| Create User | Create new user account |
| Toggle Role | Change USER/ADMIN role |
| Enable/Disable | Toggle user account status |
| Delete User | Remove user account |
| View User Logs | See logs for any user |
| Export User Logs | Export logs to Excel |

### Admin Business Rules
- Cannot delete last admin user
- Cannot disable last admin user
- Cannot demote last admin user

---

## WebSocket Real-time Updates

### How It Works
- WebSocket connection established on page load
- Server broadcasts updates to subscribed users
- Client receives real-time updates
- UI updates automatically without refresh

### WebSocket Endpoint
- **URL**: ws://localhost:8081/ws
- **Protocol**: STOMP over WebSocket
- **Fallback**: SockJS for older browsers

### Client Subscription
- **Topic**: `/topic/requests/{username}`
- **Message Format**:
```json
{
  "type": "REQUEST_UPDATE",
  "count": 100,
  "timestamp": 1681622400000
}
```

### Usage Example
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

## Best Practices

### Webhook Configuration
- Use descriptive paths
- Set appropriate status codes
- Configure delays for realistic testing
- Use template variables for dynamic responses

### Request Logs
- Regularly export logs for analysis
- Delete old logs to save space
- Use filtering to find specific requests
- Copy curl commands for testing

### Security
- Use strong passwords
- Keep API configurations private
- Monitor request logs for suspicious activity
- Export logs before deleting

---

## Troubleshooting

### Cannot Create Webhook
- Check path uniqueness (must be unique per user)
- Verify all required fields are filled
- Check status code range (100-599)
- Check delay range (0-60000ms)

### Template Variables Not Working
- Verify variable syntax: `{{variable}}`
- Check variable name is correct
- Ensure request contains expected data

### Logs Not Updating
- Check WebSocket connection
- Verify user is logged in
- Check browser console for errors

### Export Not Working
- Check browser permissions
- Verify file size limits
- Try different browser

---

## Next Steps

- [ ] [Installation Guide](./INSTALLATION.md) - Setup development environment
- [ ] [Architecture](./ARCHITECTURE.md) - Understand system design
- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy to production
