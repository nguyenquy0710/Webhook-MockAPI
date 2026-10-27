# Test Plan & Test Cases
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Test Strategy

### 1.1 Test Levels
- **Unit Tests**: Test individual service methods
- **Integration Tests**: Test controller endpoints and database interactions
- **System Tests**: Test complete user workflows
- **Acceptance Tests**: Validate business requirements

### 1.2 Test Tools
- **JUnit 5**: Unit and integration testing
- **Mockito**: Mock dependencies
- **Spring Boot Test**: Integration testing
- **Testcontainers**: Database testing
- **Selenium/WebDriver**: UI testing (optional)

### 1.3 Test Coverage Targets
- **Unit Tests**: > 80% code coverage
- **Integration Tests**: All controllers covered
- **Critical Paths**: 100% coverage

---

## 2. Unit Test Cases

### 2.1 User Service Tests

#### 2.1.1 registerUser
**Test Case**: TC-US-001
- **Description**: Register a new user with valid data
- **Input**: Valid username, email, password
- **Expected**: User created, password hashed, role set to USER
- **Assertions**: User exists in database, password is BCrypt hashed

#### 2.1.2 registerUser - Duplicate Username
**Test Case**: TC-US-002
- **Description**: Attempt to register with existing username
- **Input**: Username already exists
- **Expected**: RuntimeException thrown
- **Assertions**: Exception message contains "Username already exists"

#### 2.1.3 registerUser - Password Mismatch
**Test Case**: TC-US-003
- **Description**: Attempt to register with mismatched passwords
- **Input**: Password and confirmPassword don't match
- **Expected**: RuntimeException thrown
- **Assertions**: Exception message contains "Passwords do not match"

#### 2.1.4 findByUsername
**Test Case**: TC-US-004
- **Description**: Retrieve user by username
- **Input**: Valid username
- **Expected**: User object returned
- **Assertions**: User details match

#### 2.1.5 toggleUserRole
**Test Case**: TC-US-005
- **Description**: Toggle user role from USER to ADMIN
- **Input**: User ID
- **Expected**: Role changed to ADMIN
- **Assertions**: User role is ADMIN

#### 2.1.6 toggleUserRole - Last Admin
**Test Case**: TC-US-006
- **Description**: Attempt to demote last admin user
- **Input**: Only admin user ID
- **Expected**: RuntimeException thrown
- **Assertions**: Exception message contains "last admin"

---

### 2.2 API Config Service Tests

#### 2.2.1 saveApiConfig
**Test Case**: TC-ACS-001
- **Description**: Create new API configuration
- **Input**: Valid configuration DTO
- **Expected**: Configuration saved with timestamps
- **Assertions**: Configuration exists, timestamps set

#### 2.2.2 saveApiConfig - Update
**Test Case**: TC-ACS-002
- **Description**: Update existing API configuration
- **Input**: Configuration DTO with ID
- **Expected**: Configuration updated, updatedAt changed
- **Assertions**: Configuration updated, timestamps correct

#### 2.2.3 getApiConfigs
**Test Case**: TC-ACS-003
- **Description**: Retrieve all configurations for user
- **Input**: Username
- **Expected**: List of configurations
- **Assertions**: List size matches, ownership verified

#### 2.2.4 deleteApiConfig
**Test Case**: TC-ACS-004
- **Description**: Delete API configuration
- **Input**: Configuration ID
- **Expected**: Configuration deleted
- **Assertions**: Configuration no longer exists

#### 2.2.5 findApiConfig
**Test Case**: TC-ACS-005
- **Description**: Find configuration by path and method
- **Input**: Username, path, HTTP method
- **Expected**: Optional with matching configuration
- **Assertions**: Configuration matches path and method

---

### 2.3 Request Log Service Tests

#### 2.3.1 logRequest
**Test Case**: TC-RLS-001
- **Description**: Create request log entry
- **Input**: User, request, body, status
- **Expected**: Log entry created with all fields
- **Assertions**: Log exists, curl generated, WebSocket update sent

#### 2.3.2 getRequestLogs
**Test Case**: TC-RLS-002
- **Description**: Retrieve paginated request logs
- **Input**: Username, pageable
- **Expected**: Page of logs sorted by timestamp descending
- **Assertions**: Page size correct, sorting correct

#### 2.3.3 deleteAllRequestLogs
**Test Case**: TC-RLS-003
- **Description**: Delete all request logs for user
- **Input**: Username
- **Expected**: All logs deleted, WebSocket update sent
- **Assertions**: Log count is zero

#### 2.3.4 exportToExcel
**Test Case**: TC-RLS-004
- **Description**: Export request logs to Excel
- **Input**: Username, response
- **Expected**: Excel file generated
- **Assertions**: File created, correct columns

---

### 2.4 WebSocket Service Tests

#### 2.4.1 sendRequestUpdate
**Test Case**: TC-WSS-001
- **Description**: Broadcast request count update
- **Input**: Username
- **Expected**: Message sent to /topic/requests/{username}
- **Assertions**: Message contains type, count, timestamp

---

## 3. Integration Test Cases

### 3.1 Authentication Controller Tests

#### 3.1.1 POST /register
**Test Case**: TC-AC-001
- **Description**: Register new user
- **Method**: POST
- **Input**: Valid registration data
- **Expected**: 302 redirect to /login?registered
- **Assertions**: User created in database

#### 3.1.2 POST /register - Invalid Data
**Test Case**: TC-AC-002
- **Description**: Register with invalid data
- **Method**: POST
- **Input**: Invalid email, short password
- **Expected**: 200 OK, form re-rendered with errors
- **Assertions**: Validation errors present

#### 3.1.3 POST /login
**Test Case**: TC-AC-003
- **Description**: Login with valid credentials
- **Method**: POST
- **Input**: Valid username and password
- **Expected**: 302 redirect to /dashboard
- **Assertions**: Session created, JSESSIONID cookie set

#### 3.1.4 GET /logout
**Test Case**: TC-AC-004
- **Description**: Logout user
- **Method**: GET
- **Input**: Authenticated session
- **Expected**: 302 redirect to /login?logout
- **Assertions**: Session invalidated, cookie deleted

---

### 3.2 API Config Controller Tests

#### 3.2.1 GET /api/configs/@{username}
**Test Case**: TC-ACC-001
- **Description**: List API configurations for user
- **Method**: GET
- **Input**: Username, pagination params
- **Expected**: 200 OK with DataTables format
- **Assertions**: Configurations returned, ownership verified

#### 3.2.2 POST /api/configs
**Test Case**: TC-ACC-002
- **Description**: Create new API configuration
- **Method**: POST
- **Input**: Configuration DTO
- **Expected**: 200 OK with created configuration
- **Assertions**: Configuration saved, ID returned

#### 3.2.3 PUT /api/configs
**Test Case**: TC-ACC-003
- **Description**: Update API configuration
- **Method**: PUT
- **Input**: Configuration DTO with ID
- **Expected**: 200 OK with updated configuration
- **Assertions**: Configuration updated

#### 3.2.4 DELETE /api/configs/{id}
**Test Case**: TC-ACC-004
- **Description**: Delete API configuration
- **Method**: DELETE
- **Input**: Configuration ID
- **Expected**: 204 No Content
- **Assertions**: Configuration deleted

---

### 3.3 Request Log Controller Tests

#### 3.3.1 GET /api/logs/@{username}
**Test Case**: TC-RLC-001
- **Description**: List request logs
- **Method**: GET
- **Input**: Username, pagination
- **Expected**: 200 OK with paginated logs
- **Assertions**: Logs returned, ownership verified

#### 3.3.2 GET /api/logs/@{username}/count
**Test Case**: TC-RLC-002
- **Description**: Get request log count
- **Method**: GET
- **Input**: Username
- **Expected**: 200 OK with count
- **Assertions**: Count matches database

#### 3.3.3 DELETE /api/logs/@{username}
**Test Case**: TC-RLC-003
- **Description**: Delete all request logs
- **Method**: DELETE
- **Input**: Username
- **Expected**: 204 No Content
- **Assertions**: Logs deleted

---

### 3.4 Admin Controller Tests

#### 3.4.1 GET /admin/users
**Test Case**: TC-AC-005
- **Description**: List all users (admin only)
- **Method**: GET
- **Input**: Admin session
- **Expected**: 200 OK with user list
- **Assertions**: All users returned

#### 3.4.2 POST /admin/users/{id}/delete
**Test Case**: TC-AC-006
- **Description**: Delete user (admin only)
- **Method**: POST
- **Input**: User ID
- **Expected**: 302 redirect to /admin/users
- **Assertions**: User deleted

#### 3.4.3 POST /admin/users/{id}/toggle-role
**Test Case**: TC-AC-007
- **Description**: Toggle user role (admin only)
- **Method**: POST
- **Input**: User ID
- **Expected**: 302 redirect to /admin/users
- **Assertions**: Role toggled

---

## 4. System Test Cases

### 4.1 User Registration Flow

**Test Case**: TC-SYS-001
- **Description**: Complete user registration and login
- **Steps**:
  1. Navigate to /register
  2. Fill registration form
  3. Submit form
  4. Verify redirect to /login
  5. Login with credentials
  6. Verify redirect to /dashboard
- **Expected**: User authenticated, dashboard displayed

---

### 4.2 Webhook Configuration Flow

**Test Case**: TC-SYS-002
- **Description**: Create, view, update, and delete webhook configuration
- **Steps**:
  1. Navigate to /dashboard
  2. Click "Create Webhook"
  3. Fill configuration form
  4. Submit and verify success
  5. View configuration in list
  6. Edit configuration
  7. Delete configuration
- **Expected**: All CRUD operations successful

---

### 4.3 Webhook Request Flow

**Test Case**: TC-SYS-003
- **Description**: Send request to webhook and verify logging
- **Steps**:
  1. Create webhook configuration
  2. Send HTTP request to webhook URL
  3. Verify response matches configuration
  4. Check request logs page
  5. Verify log entry created
  6. Verify curl command generated
- **Expected**: Request processed, log created

---

### 4.4 Real-time Updates Flow

**Test Case**: TC-SYS-004
- **Description**: Verify WebSocket real-time updates
- **Steps**:
  1. Open request logs page
  2. Send webhook request from another tab
  3. Verify log count updates without refresh
- **Expected**: Real-time update received

---

### 4.5 Admin User Management Flow

**Test Case**: TC-SYS-005
- **Description**: Admin creates and manages users
- **Steps**:
  1. Login as admin
  2. Navigate to /admin/users
  3. Create new user
  4. Toggle user role
  5. Delete user
- **Expected**: All admin operations successful

---

## 5. Performance Test Cases

### 5.1 Concurrent Webhook Requests

**Test Case**: TC-PERF-001
- **Description**: Handle 100 concurrent webhook requests
- **Method**: JMeter or Gatling
- **Input**: 100 concurrent POST requests
- **Expected**: All requests processed within 1 second (excluding delay)
- **Metrics**: Response time, success rate

---

### 5.2 Database Query Performance

**Test Case**: TC-PERF-002
- **Description**: Verify database query performance
- **Method**: Spring Data JPA with profiling
- **Input**: Query with 10,000+ log entries
- **Expected**: Query completes in < 100ms
- **Metrics**: Query execution time

---

### 5.3 WebSocket Latency

**Test Case**: TC-PERF-003
- **Description**: Verify WebSocket update latency
- **Method**: WebSocket client test
- **Input**: Send request, measure WebSocket message
- **Expected**: Update delivered within 500ms
- **Metrics**: Message delivery time

---

## 6. Security Test Cases

### 6.1 Authentication Tests

**Test Case**: TC-SEC-001
- **Description**: Verify password hashing
- **Method**: Database inspection
- **Input**: Registered user
- **Expected**: Password stored as BCrypt hash
- **Assertions**: Password not in plain text

---

**Test Case**: TC-SEC-002
- **Description**: Verify session security
- **Method**: Browser inspection
- **Input**: Login session
- **Expected**: JSESSIONID cookie with HttpOnly flag
- **Assertions**: Cookie flags correct

---

### 6.2 Authorization Tests

**Test Case**: TC-SEC-003
- **Description**: Verify user data isolation
- **Method**: API testing
- **Input**: User A's configuration accessed by User B
- **Expected**: 403 Forbidden
- **Assertions**: Access denied

---

**Test Case**: TC-SEC-004
- **Description**: Verify admin-only endpoints
- **Method**: API testing
- **Input**: Regular user accessing /admin/users
- **Expected**: 403 Forbidden
- **Assertions**: Access denied

---

### 6.3 Input Validation Tests

**Test Case**: TC-SEC-005
- **Description**: Verify SQL injection prevention
- **Method**: API testing
- **Input**: Malicious SQL in path parameter
- **Expected**: 400 Bad Request or sanitized input
- **Assertions**: No SQL error in response

---

**Test Case**: TC-SEC-006
- **Description**: Verify XSS prevention
- **Method**: API testing
- **Input**: Script tag in response body
- **Expected**: Script escaped or rejected
- **Assertions**: No script execution

---

## 7. Test Execution Plan

### 7.1 Test Environment
- **Database**: H2 in-memory for tests
- **Test Framework**: JUnit 5 + Spring Boot Test
- **Coverage Tool**: JaCoCo

### 7.2 Test Execution Order
1. Unit tests (fastest, isolated)
2. Integration tests (database, controllers)
3. System tests (full workflows)
4. Performance tests (after stability)

### 7.3 CI/CD Integration
- **Command**: `./mvnw test`
- **Coverage Report**: `./mvnw clean test jacoco:report`
- **Fail Fast**: Tests fail on first error

### 7.4 Test Maintenance
- **New Features**: Write tests before code (TDD)
- **Bug Fixes**: Add regression tests
- **Refactoring**: Update tests to match new implementation

---

## 8. Test Data Management

### 8.1 Test Data Setup
- **Users**: admin (ADMIN), testuser (USER)
- **API Configurations**: Sample configurations for testing
- **Request Logs**: Sample logs for pagination tests

### 8.2 Test Data Cleanup
- **After Each Test**: Delete test data
- **After Each Class**: Reset database
- **Before Suite**: Create base test data

---

## 9. Test Reporting

### 9.1 Test Results
- **Format**: JUnit XML
- **Location**: `target/surefire-reports/`
- **Coverage**: `target/site/jacoco/`

### 9.2 Test Metrics
- **Pass Rate**: Percentage of passing tests
- **Coverage**: Code coverage percentage
- **Duration**: Total test execution time

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
