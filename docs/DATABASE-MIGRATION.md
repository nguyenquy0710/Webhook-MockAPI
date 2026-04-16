# Database Migration Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Overview](#overview)
- [Database Schema](#database-schema)
- [Flyway Configuration](#flyway-configuration)
- [Migration Files](#migration-files)
- [Running Migrations](#running-migrations)
- [Troubleshooting](#troubleshooting)
- [Backup and Restore](#backup-and-restore)

---

## Overview

### Database Type
- **Type**: H2 Database (file-based)
- **Version**: 2.1.214
- **Persistence**: File-based at `./data/mockwebhook`
- **Migration Tool**: Flyway 8.0+

### Connection Configuration
```properties
spring.datasource.url=jdbc:h2:file:./data/mockwebhook
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
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

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX idx_username (username)
- UNIQUE INDEX idx_email (email)

---

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

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- INDEX idx_user_id (user_id)
- INDEX idx_path_method (path, method)

---

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

**Indexes**:
- PRIMARY KEY (id)
- FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
- INDEX idx_user_id (user_id)
- INDEX idx_timestamp (timestamp)

---

## Flyway Configuration

### Configuration Properties
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.baseline-description=Existing database baseline
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
spring.flyway.ignore-missing-migrations=false
spring.flyway.ignore-ignored-migrations=false
spring.flyway.ignore-pending-migrations=false
spring.flyway.ignore-future-migrations=true
spring.flyway.clean-on-validation-error=false
```

### Migration Types
- **V{n}__{description}.sql**: Standard migration
- **V{n}__{description}.sql**: Versioned migration
- **R__{description}.sql**: Repeatable migration

---

## Migration Files

### Current Migrations
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_delay_ms_column.sql
└── V3__Add_delay_ms_column.sql
```

### V1__Initial_schema.sql
Creates the initial database schema with all three tables and indexes.

**Changes**:
- Creates `users` table with authentication fields
- Creates `api_configs` table with webhook configuration
- Creates `request_logs` table for request logging
- Adds indexes for performance optimization

### V2__Add_delay_ms_column.sql
Adds response delay functionality.

**Changes**:
- Adds `delay_ms` column to `api_configs` table
- Sets default value to 0
- Adds column comment

### V3__Add_delay_ms_column.sql
Fixes migration issues and ensures data consistency.

**Changes**:
- Adds missing `role` and `enabled` columns to `users` table
- Sets default values for existing records
- Ensures `delay_ms` column exists in `api_configs`

---

## Running Migrations

### Automatic Migration
Migrations run automatically on application startup when:
- `spring.flyway.enabled=true`
- Application starts

### Manual Migration Commands
```bash
# Run migration
./mvnw flyway:migrate

# Repair migration (if needed)
./mvnw flyway:repair

# Validate migration
./mvnw flyway:validate

# Clean database (WARNING: deletes all data)
./mvnw flyway:clean

# Info about migrations
./mvnw flyway:info
```

### Using Migration Scripts
```bash
# Validate migration
./validate_migration.sh

# Repair database
./repair_database.sh
```

---

## Troubleshooting

### Migration Validation Error
```bash
# Run repair script
./repair_database.sh

# Or run Flyway repair manually
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=
```

### Database File Not Found
```bash
# Start application to create database
./mvnw spring-boot:run

# Or create data directory
mkdir -p ./data
```

### Corrupted Database
```bash
# Delete data directory (WARNING: loses all data)
rm -rf ./data/

# Restart application to recreate database
./mvnw spring-boot:run
```

### Migration Conflict
```bash
# Check migration history
./mvnw flyway:info

# Repair migration
./mvnw flyway:repair
```

---

## Backup and Restore

### Database Backup
```bash
# Backup H2 database
cp ./data/mockwebhook.mv.db ./backup/mockwebhook-$(date +%Y%m%d).mv.db

# Or use Flyway backup
./mvnw flyway:backup -Dflyway.url=jdbc:h2:file:./data/mockwebhook
```

### Automated Backup Script
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="./backup"
DB_PATH="./data/mockwebhook"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory if not exists
mkdir -p $BACKUP_DIR

# Backup database
cp ${DB_PATH}.mv.db ${BACKUP_DIR}/mockwebhook_${DATE}.mv.db

# Keep only last 30 days of backups
find $BACKUP_DIR -name "mockwebhook_*.mv.db" -mtime +30 -delete

echo "Backup completed: mockwebhook_${DATE}.mv.db"
```

### Database Recovery
```bash
# Restore from backup
cp ./backup/mockwebhook-20240101.mv.db ./data/mockwebhook.mv.db

# Repair Flyway history if needed
./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook
```

### Docker Backup
```bash
# Backup Docker volume
docker run --rm -v mockapi_data:/data -v $(pwd):/backup ubuntu tar czf /backup/backup.tar.gz /data

# Restore from backup
docker run --rm -v mockapi_data:/data -v $(pwd):/backup ubuntu tar xzf /backup/backup.tar.gz -C /
```

---

## Creating New Migrations

### Migration File Naming
- Format: `V{n}__{description}.sql`
- Example: `V4__Add_retry_configuration.sql`

### Example Migration
```sql
-- Add new column to api_configs table
ALTER TABLE api_configs ADD COLUMN retry_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE api_configs ADD COLUMN max_retries INTEGER DEFAULT 3;
ALTER TABLE api_configs ADD COLUMN retry_delay_ms INTEGER DEFAULT 1000;
ALTER TABLE api_configs ADD COLUMN retry_on_status_codes CLOB DEFAULT '[408, 429, 500, 502, 503, 504]';
```

### Migration Best Practices
- Use `IF NOT EXISTS` for column additions
- Set default values for new columns
- Add indexes for frequently queried columns
- Test migrations on development database first
- Document changes in migration file comments

---

## Database Maintenance

### Regular Maintenance Tasks
- **Backup**: Daily automated backups
- **Cleanup**: Remove old logs (30 days retention)
- **Optimization**: Rebuild indexes periodically
- **Monitoring**: Track database size and query performance

### Database Optimization
```sql
-- Rebuild indexes
ANALYZE;

-- Check table sizes
SELECT TABLE_NAME, ROW_COUNT, DATA_LENGTH 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'PUBLIC';
```

---

## Migration History

### Current Version
- **Version**: 3
- **Description**: Add missing columns and data cleanup

### Migration History Table
```sql
-- Check migration history
SELECT "installed_rank", "version", "description", "success" 
FROM "flyway_schema_history" 
ORDER BY "installed_rank";
```

---

## Next Steps

- [ ] [Installation Guide](./INSTALLATION.md) - Setup development environment
- [ ] [Architecture](./ARCHITECTURE.md) - Understand system design
- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy to production
