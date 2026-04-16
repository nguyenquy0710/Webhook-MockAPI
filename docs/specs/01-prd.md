# Product Requirements Document (PRD)
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Introduction

### 1.1 Purpose
WebHookMock is a web-based webhook testing and simulation platform that enables developers to create, manage, and test webhook APIs without requiring external services. The application provides a complete environment for simulating webhook endpoints, monitoring incoming requests, and analyzing webhook behavior.

### 1.2 Scope
WebHookMock provides:
- User registration and authentication system
- Custom webhook endpoint creation with configurable responses
- Real-time request logging and monitoring
- WebSocket-based live updates
- Excel export functionality for request logs
- Swagger documentation for API endpoints
- Multi-user isolation with username-based URL namespaces

### 1.3 Definitions
- **Webhook**: A method for augmenting or altering the behavior of a web page, or web application, with custom callbacks
- **API Configuration**: User-defined settings that specify how incoming webhook requests should be handled
- **Request Log**: A record of all incoming webhook requests including headers, body, and response details

### 1.4 Overview
WebHookMock allows developers to create custom webhook endpoints with specific response configurations. When external services send HTTP requests to these endpoints, the system logs the request details and returns the configured response. This enables developers to test webhook integrations without setting up complex infrastructure.

---

## 2. Stakeholders

| Role | Description | Responsibilities |
|------|-------------|------------------|
| Developer | Primary user of the system | Create webhook endpoints, test integrations, view logs |
| Admin | System administrator | Manage users, monitor system usage, view all logs |
| QA Engineer | Testing team member | Verify webhook behavior, validate request/response formats |

---

## 3. Product Goals

### 3.1 Primary Goals
1. **Ease of Use**: Simple interface for creating and managing webhook endpoints
2. **Real-time Feedback**: Immediate visibility into incoming webhook requests
3. **Accurate Simulation**: Faithful representation of real webhook behavior
4. **Data Persistence**: Reliable storage of request logs for analysis
5. **Multi-user Support**: Isolated environments for different users

### 3.2 Success Metrics
- Time to create first webhook endpoint: < 2 minutes
- Request log latency: < 1 second
- System uptime: 99.5% (excluding maintenance)
- User satisfaction: > 4.0/5.0

---

## 4. Functional Requirements

### 4.1 User Management

#### 4.1.1 Registration
- **FR-UM-001**: System shall allow new users to register with username, email, and password
- **FR-UM-002**: System shall validate username uniqueness
- **FR-UM-003**: System shall validate email format and uniqueness
- **FR-UM-004**: System shall hash passwords using BCrypt before storage

#### 4.1.2 Authentication
- **FR-UM-005**: System shall provide login page with username/password authentication
- **FR-UM-006**: System shall maintain user session using secure cookies
- **FR-UM-007**: System shall redirect authenticated users to dashboard
- **FR-UM-008**: System shall allow users to logout and invalidate session

#### 4.1.3 Password Management
- **FR-UM-009**: System shall allow users to change their password
- **FR-UM-010**: System shall verify current password before allowing change
- **FR-UM-011**: System shall require password confirmation for security

### 4.2 Webhook Configuration

#### 4.2.1 Create API Configuration
- **FR-WC-001**: System shall allow users to create new webhook configurations
- **FR-WC-002**: System shall require path specification for webhook endpoint
- **FR-WC-003**: System shall allow selection of HTTP method (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD)
- **FR-WC-004**: System shall allow configuration of response status code (100-599)
- **FR-WC-005**: System shall allow configuration of response content type
- **FR-WC-006**: System shall allow configuration of response body with template variables
- **FR-WC-007**: System shall allow configuration of response headers
- **FR-WC-008**: System shall allow configuration of response delay (0-60000ms)

#### 4.2.2 List API Configurations
- **FR-WC-009**: System shall display list of all webhook configurations for current user
- **FR-WC-010**: System shall support pagination for large result sets
- **FR-WC-011**: System shall support search/filtering of configurations

#### 4.2.3 Update API Configuration
- **FR-WC-012**: System shall allow users to edit existing webhook configurations
- **FR-WC-013**: System shall verify user ownership before allowing updates
- **FR-WC-014**: System shall update timestamp on configuration modification

#### 4.2.4 Delete API Configuration
- **FR-WC-015**: System shall allow users to delete webhook configurations
- **FR-WC-016**: System shall verify user ownership before allowing deletion
- **FR-WC-017**: System shall cascade delete related request logs

#### 4.2.5 Backup and Restore
- **FR-WC-018**: System shall allow users to backup all API configurations to JSON file
- **FR-WC-019**: System shall allow users to restore API configurations from JSON backup

### 4.3 Webhook Request Handling

#### 4.3.1 Request Processing
- **FR-WR-001**: System shall accept HTTP requests to `/api/@{username}/{path}`
- **FR-WR-002**: System shall match incoming requests to configured webhook endpoints
- **FR-WR-003**: System shall apply configured delay before responding
- **FR-WR-004**: System shall process template variables in response body
- **FR-WR-005**: System shall apply configured response headers
- **FR-WR-006**: System shall return configured status code

#### 4.3.2 Template Variables
- **FR-WR-007**: System shall support `{{headers.*}}` for request headers
- **FR-WR-008**: System shall support `{{params.*}}` for query parameters
- **FR-WR-009**: System shall support `{{method}}` for HTTP method
- **FR-WR-010**: System shall support `{{path}}` for request path
- **FR-WR-011**: System shall support `{{status}}` for response status
- **FR-WR-012**: System shall support `{{remoteAddr}}` for client IP
- **FR-WR-013**: System shall support `{{cookies.*}}` for cookie values

### 4.4 Request Logging

#### 4.4.1 Log Creation
- **FR-RL-001**: System shall create request log entry for each incoming webhook request
- **FR-RL-002**: System shall store HTTP method in log entry
- **FR-RL-003**: System shall store request path in log entry
- **FR-RL-004**: System shall store source IP address in log entry
- **FR-RL-005**: System shall store query parameters in log entry
- **FR-RL-006**: System shall store request headers in log entry
- **FR-RL-007**: System shall store request body in log entry
- **FR-RL-008**: System shall store response status code in log entry
- **FR-RL-009**: System shall store response body in log entry
- **FR-RL-010**: System shall store timestamp of request arrival
- **FR-RL-011**: System shall generate curl command for each request

#### 4.4.2 Log Retrieval
- **FR-RL-012**: System shall allow users to retrieve their request logs
- **FR-RL-013**: System shall support pagination for log retrieval
- **FR-RL-014**: System shall support filtering logs by path
- **FR-RL-015**: System shall return logs sorted by timestamp (descending)

#### 4.4.3 Log Management
- **FR-RL-016**: System shall allow users to delete all their request logs
- **FR-RL-017**: System shall notify user via WebSocket when logs are deleted

### 4.5 Real-time Updates

#### 4.5.1 WebSocket Integration
- **FR-RT-001**: System shall provide WebSocket endpoint at `/ws`
- **FR-RT-002**: System shall broadcast request count updates to subscribed users
- **FR-RT-003**: System shall use STOMP protocol for WebSocket communication
- **FR-RT-004**: System shall support SockJS fallback for older browsers

### 4.6 Admin Functions

#### 4.6.1 User Management
- **FR-AD-001**: System shall allow admins to view all users
- **FR-AD-002**: System shall allow admins to create new users
- **FR-AD-003**: System shall allow admins to delete users
- **FR-AD-004**: System shall allow admins to toggle user roles (USER/ADMIN)
- **FR-AD-005**: System shall allow admins to enable/disable users
- **FR-AD-006**: System shall prevent deletion of last admin user
- **FR-AD-007**: System shall prevent disabling of last admin user

#### 4.6.2 Log Viewing
- **FR-AD-008**: System shall allow admins to view request logs for any user
- **FR-AD-009**: System shall allow admins to export any user's logs to Excel
- **FR-AD-010**: System shall allow admins to filter logs by path

### 4.7 API Documentation

#### 4.7.1 Swagger Integration
- **FR-AD-011**: System shall generate Swagger documentation for REST APIs
- **FR-AD-012**: System shall provide Swagger UI at `/swagger-ui`
- **FR-AD-013**: System shall provide per-user Swagger specs at `/swagger/@{username}`
- **FR-AD-014**: System shall expose raw Swagger JSON at `/swagger.json`

---

## 5. Non-Functional Requirements

### 5.1 Performance
- **NFR-PERF-001**: System shall handle concurrent requests with minimal latency
- **NFR-PERF-002**: Request logs shall be written asynchronously
- **NFR-PERF-003**: WebSocket updates shall be delivered within 500ms
- **NFR-PERF-004**: System shall support at least 100 concurrent users

### 5.2 Security
- **NFR-SEC-001**: System shall use HTTPS in production
- **NFR-SEC-002**: System shall hash passwords using BCrypt with cost factor 12
- **NFR-SEC-003**: System shall validate all user inputs
- **NFR-SEC-004**: System shall prevent CSRF attacks (disabled for webhook APIs)
- **NFR-SEC-005**: System shall enforce user-level data isolation
- **NFR-SEC-006**: System shall log security events

### 5.3 Availability
- **NFR-AVAIL-001**: System shall be accessible 24/7 (excluding maintenance windows)
- **NFR-AVAIL-002**: System shall recover automatically from database connection failures
- **NFR-AVAIL-003**: System shall provide graceful degradation for non-critical features

### 5.4 Reliability
- **NFR-REL-001**: System shall persist all data to H2 database
- **NFR-REL-002**: System shall handle database migration automatically using Flyway
- **NFR-REL-003**: System shall log all errors with stack traces

### 5.5 Maintainability
- **NFR-MAINT-001**: System shall follow clean architecture principles
- **NFR-MAINT-002**: System shall have unit test coverage > 80%
- **NFR-MAINT-003**: System shall use consistent code style
- **NFR-MAINT-004**: System shall document all public APIs

### 5.6 Usability
- **NFR-USAB-001**: System shall have intuitive user interface
- **NFR-USAB-002**: System shall provide clear error messages
- **NFR-USAB-003**: System shall use responsive design (Bootstrap 5)
- **NFR-USAB-004**: System shall support modern browsers (Chrome, Firefox, Safari, Edge)

---

## 6. User Interface Requirements

### 6.1 Dashboard
- **UI-001**: Dashboard shall display user's webhook configurations
- **UI-002**: Dashboard shall display recent request log count
- **UI-003**: Dashboard shall provide quick access to create new webhook
- **UI-004**: Dashboard shall provide navigation to request logs

### 6.2 Webhook Configuration Form
- **UI-005**: Form shall validate all required fields before submission
- **UI-006**: Form shall provide helpful error messages
- **UI-007**: Form shall support template variable preview
- **UI-008**: Form shall allow testing of configuration before saving

### 6.3 Request Logs View
- **UI-009**: Logs view shall display table of request entries
- **UI-UI-010**: Logs view shall show pagination controls
- **UI-011**: Logs view shall allow filtering by path
- **UI-012**: Logs view shall display curl command for each request
- **UI-013**: Logs view shall show real-time updates via WebSocket

### 6.4 Admin Panel
- **UI-014**: Admin panel shall be accessible only to ADMIN role
- **UI-015**: Admin panel shall display user management interface
- **UI-016**: Admin panel shall display user statistics

---

## 7. Data Requirements

### 7.1 User Data
- **DAT-UM-001**: System shall store username (unique, 3-20 chars)
- **DAT-UM-002**: System shall store email (unique, valid format)
- **DAT-UM-003**: System shall store hashed password
- **DAT-UM-004**: System shall store user role (USER/ADMIN)
- **DAT-UM-005**: System shall store user enabled status
- **DAT-UM-006**: System shall store creation timestamp

### 7.2 API Configuration Data
- **DAT-WC-001**: System shall store path (required)
- **DAT-WC-002**: System shall store HTTP method (required)
- **DAT-WC-003**: System shall store content type
- **DAT-WC-004**: System shall store status code
- **DAT-WC-005**: System shall store response body (CLOB)
- **DAT-WC-006**: System shall store response headers (JSON, CLOB)
- **DAT-WC-007**: System shall store delay in milliseconds
- **DAT-WC-008**: System shall store creation timestamp
- **DAT-WC-009**: System shall store update timestamp

### 7.3 Request Log Data
- **DAT-RL-001**: System shall store HTTP method
- **DAT-RL-002**: System shall store request path
- **DAT-RL-003**: System shall store source IP address
- **DAT-RL-004**: System shall store query parameters (CLOB)
- **DAT-RL-005**: System shall store request headers (JSON, CLOB)
- **DAT-RL-006**: System shall store request body (CLOB)
- **DAT-RL-007**: System shall store response status code
- **DAT-RL-008**: System shall store response body (CLOB)
- **DAT-RL-009**: System shall store timestamp
- **DAT-RL-010**: System shall store curl command (CLOB)

---

## 8. Assumptions and Constraints

### 8.1 Assumptions
- Users have basic understanding of HTTP protocols
- Users have access to modern web browsers
- External webhook services can reach the application via public URL
- Database size will not exceed 10GB
- Network latency between users and server < 200ms

### 8.2 Constraints
- System must run on Java 17
- Database must be H2 (file-based)
- Frontend must use Thymeleaf and Bootstrap 5
- No external authentication providers (OAuth, SAML)
- No multi-tenancy support (single database per instance)

---

## 9. Future Enhancements

### 9.1 Phase 2
- Webhook retry configuration
- Webhook signature verification
- Webhook request replay
- Advanced filtering and search
- Export to CSV/JSON/PDF

### 9.2 Phase 3
- Team collaboration features
- Shared webhook configurations
- Webhook analytics and insights
- API usage quotas
- Rate limiting per user

### 9.3 Phase 4
- Webhook testing automation
- Mock server for multiple endpoints
- Integration with CI/CD pipelines
- Advanced template engine
- Custom domain support

---

## 10. Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Product Owner | | | |
| Technical Lead | | | |
| QA Lead | | | |

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
