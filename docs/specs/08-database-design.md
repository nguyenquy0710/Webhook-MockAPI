# Database Design
## WebHookMock - Webhook Testing Tool

**Version:** 1.0  
**Date:** 2026-04-16  
**Status:** Final  
**Author:** WebHookMock Development Team

---

## 1. Database Overview

### 1.1 Database Type
- **Type**: H2 Database (file-based)
- **Version**: 2.1.214
- **Persistence**: File-based at `./data/mockwebhook`
- **Migration Tool**: Flyway 8.0+

### 1.2 Connection Configuration
```properties
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

---

## 2. Database Schema

### 2.1 Tables

#### 2.1.1 users
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

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX idx_username (username)
- UNIQUE INDEX idx_email (email)

**Sample Data**:
```sql
INSERT INTO users (username, password, email, role, enabled) 
VALUES ('admin', '$2a$12$...', 'admin@example.com', 'ADMIN', TRUE);
```

---

#### 2.1.2 api_configs
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

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- INDEX idx_user_id (user_id)
- INDEX idx_path_method (path, method)

**Sample Data**:
```sql
INSERT INTO api_configs (user_id, path, method, content_type, status_code, response_body, response_headers, delay_ms)
VALUES (1, '/webhook/test', 'POST', 'application/json', 200, '{"message": "Hello"}', '{"X-Custom": "value"}', 0);
```

---

#### 2.1.3 request_logs
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

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- INDEX idx_user_id (user_id)
- INDEX idx_timestamp (timestamp)

**Sample Data**:
```sql
INSERT INTO request_logs (user_id, method, path, source_ip, query_params, request_headers, request_body, response_status, response_body, curl)
VALUES (1, 'POST', '/webhook/test', '192.168.1.1', 'key=value', '{"Content-Type":"application/json"}', '{"data":"test"}', 200, '{"message":"Hello"}', 'curl -X POST ...');
```

---

## 3. Database Migrations

### 3.1 Migration History

#### V1__Initial_schema.sql
Creates the initial database schema with all three tables and indexes.

**Changes**:
- Creates `users` table with authentication fields
- Creates `api_configs` table with webhook configuration
- Creates `request_logs` table for request logging
- Adds indexes for performance optimization

#### V2__Add_delay_ms_column.sql
Adds response delay functionality.

**Changes**:
- Adds `delay_ms` column to `api_configs` table
- Sets default value to 0
- Adds column comment

#### V3__Add_delay_ms_column.sql
Fixes migration issues and ensures data consistency.

**Changes**:
- Adds missing `role` and `enabled` columns to `users` table
- Sets default values for existing records
- Ensures `delay_ms` column exists in `api_configs`

---

## 4. Entity Relationships

```
┌─────────────┐
│   users     │
├─────────────┤
│ id (PK)     │
│ username    │
│ password    │
│ email       │
│ role        │
│ enabled     │
│ created_at  │
└──────┬──────┘
       │ 1
       │
       │ N
       │
┌──────▼─────────────┐
│   api_configs      │
├────────────────────┤
│ id (PK)            │
│ user_id (FK)       │
│ path               │
│ method             │
│ content_type       │
│ status_code        │
│ response_body      │
│ response_headers   │
│ delay_ms           │
│ created_at         │
│ updated_at         │
└────────────────────┘

┌─────────────┐
│   users     │
├─────────────┤
│ id (PK)     │
│ username    │
│ password    │
│ email       │
│ role        │
│ enabled     │
│ created_at  │
└──────┬──────┘
       │ 1
       │
       │ N
       │
┌──────▼─────────────┐
│   request_logs     │
├────────────────────┤
│ id (PK)            │
│ user_id (FK)       │
│ method             │
│ path               │
│ source_ip          │
│ query_params       │
│ request_headers    │
│ request_body       │
│ timestamp          │
│ response_status    │
│ response_body      │
│ curl               │
└────────────────────┘
```

---

## 5. Data Types

### 5.1 Primary Data Types
- **BIGINT**: Auto-incrementing IDs
- **VARCHAR(255)**: Usernames, passwords, emails
- **VARCHAR(10)**: HTTP methods
- **VARCHAR(50)**: Roles
- **VARCHAR(100)**: Content types
- **VARCHAR(45)**: IP addresses (IPv6 compatible)

### 5.2 Large Object Types
- **CLOB**: Response bodies, headers, query params, curl commands

### 5.3 Numeric Types
- **INTEGER**: Status codes, delays
- **BOOLEAN**: Enabled status

### 5.4 Temporal Types
- **TIMESTAMP**: Creation/update timestamps, request timestamps

---

## 6. Constraints

### 6.1 Primary Keys
- All tables have auto-incrementing BIGINT primary keys

### 6.2 Foreign Keys
- `api_configs.user_id` → `users.id` (ON DELETE CASCADE)
- `request_logs.user_id` → `users.id` (ON DELETE CASCADE)

### 6.3 Unique Constraints
- `users.username` must be unique
- `users.email` must be unique

### 6.4 Not Null Constraints
- All required fields marked as NOT NULL

### 6.5 Check Constraints
- Status codes: 100-599 (validated in application layer)
- Delay: 0-60000ms (validated in application layer)

---

## 7. Indexing Strategy

### 7.1 Clustered Indexes
- Primary keys on all tables

### 7.2 Secondary Indexes
- `idx_api_configs_user_id`: User-based configuration queries
- `idx_api_configs_path_method`: Path and method matching
- `idx_request_logs_user_id`: User-based log queries
- `idx_request_logs_timestamp`: Timestamp-based sorting

### 7.3 Composite Indexes
- `idx_api_configs_path_method`: Optimizes webhook endpoint matching

---

## 8. Data Integrity

### 8.1 Cascade Deletes
- Deleting a user automatically deletes their API configurations
- Deleting a user automatically deletes their request logs

### 8.2 Transaction Management
- All database operations use Spring's `@Transactional` annotation
- BCrypt password hashing is atomic
- Configuration updates are transactional

### 8.3 Concurrency Control
- Optimistic locking via JPA versioning (if needed)
- Database-level constraints prevent data corruption

---

## 9. Backup and Recovery

### 9.1 Database File Location
- Default: `./data/mockwebhook.mv.db`
- Configuration: `spring.datasource.url`

### 9.2 Backup Strategy
- Regular file system backups
- Flyway migration history preserved
- H2 built-in backup commands available

### 9.3 Recovery Procedures
- Restore database file from backup
- Run `flyway repair` if migration history is corrupted
- Delete `./data/` directory for fresh start (if needed)

---

## 10. Performance Optimization

### 10.1 Query Optimization
- Use indexed columns in WHERE clauses
- Avoid SELECT * (select only needed columns)
- Use pagination for large result sets

### 10.2 Connection Pooling
- H2 in-memory mode for development
- File-based mode for production
- Connection pool size: 10 (default)

### 10.3 Caching
- Hibernate second-level cache (configurable)
- Query result caching for frequent queries

---

*Document Version: 1.0*  
*Last Updated: 2026-04-16*
