# Database Migration Guide

## Overview
This guide helps you migrate your existing Webhook MockAPI database to the latest version.

## What's New in This Version
- Added `delay_ms` column to `api_configs` table for response delay functionality
- Improved database migration system using Flyway
- Better database versioning and migration tracking

## Migration Process

### Automatic Migration (Recommended)
The application now uses Flyway for automatic database migrations. When you start the application:

1. **Backup your database** (important!)
   ```bash
   # Your database is located at: ./data/mockwebhook.mv.db
   cp ./data/mockwebhook.mv.db ./data/mockwebhook_backup_$(date +%Y%m%d_%H%M%S).mv.db
   ```

2. **Start the application**
   ```bash
   java -jar webhook-mockapi.jar
   ```
   
3. **Check migration logs**
   Look for migration status in the application logs:
   ```
   INFO - Current database version: 3
   INFO - Database is up to date. All migrations have been applied.
   ```

### Manual Migration (If needed)
If automatic migration fails, you can apply migrations manually:

1. **Access H2 Console**
   - Start the application
   - Go to: `http://localhost:8081/h2-console`
   - JDBC URL: `jdbc:h2:file:./data/mockwebhook`
   - Username: `sa`
   - Password: `password`

2. **Run migration SQL manually**
   ```sql
   -- Add delay_ms column if it doesn't exist
   ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS delay_ms INTEGER DEFAULT 0;
   
   -- Update NULL values
   UPDATE api_configs SET delay_ms = 0 WHERE delay_ms IS NULL;
   ```

## Troubleshooting

### Common Issues

1. **Migration failed: Table already exists**
   - This is normal for existing databases
   - Flyway will handle this automatically

2. **Column already exists error**
   - The migration scripts use `IF NOT EXISTS` to handle this
   - Safe to ignore these warnings

3. **Database locked error**
   - Stop the application completely
   - Wait a few seconds
   - Restart the application

### Rollback (Emergency)
If you need to rollback to your old database:

1. Stop the application
2. Restore your backup:
   ```bash
   cp ./data/mockwebhook_backup_YYYYMMDD_HHMMSS.mv.db ./data/mockwebhook.mv.db
   ```
3. Use the previous version of the application

## Migration Status Check

You can check the migration status by looking at the application logs when it starts:

- ✅ `Database is up to date` - All good!
- ⚠️ `Pending migrations` - Migrations need to be applied
- ❌ `Migration error` - Check the error details

## Support
If you encounter issues with migration, please:
1. Check the application logs
2. Backup your database before trying fixes
3. Report issues with log details

---
**Important**: Always backup your database before running migrations!