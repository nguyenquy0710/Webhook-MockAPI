# Database Migration Guide

How WebHookMock stores its data, how the schema is upgraded, and how to back up, restore and maintain the database.

## Contents

- [How it works](#how-it-works)
- [Migration history](#migration-history)
- [Upgrading](#upgrading)
- [Backup and restore](#backup-and-restore)
- [Checking the migration state](#checking-the-migration-state)
- [Maintenance](#maintenance)
- [Writing a new migration](#writing-a-new-migration)
- [Troubleshooting](#troubleshooting)
- [Rolling back](#rolling-back)
- [Using the H2 console](#using-the-h2-console)
- [Scripts](#scripts)

## How it works

- The database is an **H2 file database** at `./data/mockwebhook` (files `mockwebhook.mv.db`, plus `mockwebhook.trace.db` for H2 diagnostics). With Docker, `./data` is a mounted volume.
- Schema changes are **Flyway migrations** in `src/main/resources/db/migration/` (`V<number>__<Description>.sql`). They run automatically when the application starts, before the application uses the database.
- **Hibernate also updates the schema** (`spring.jpa.hibernate.ddl-auto=update`): after Flyway, it adds columns and tables that an entity needs and that are missing. It never drops anything. Because of this, **migrations must be idempotent** (`IF NOT EXISTS`), otherwise a column that Hibernate already created would make the migration fail.
- Existing databases from before Flyway are handled by `baseline-on-migrate=true` with `baseline-version=1`: a database without migration history is treated as being at version 1, and only the later migrations are applied.
- After startup, `DatabaseMigrationConfig` logs the migration state. It also tries a Flyway **repair** followed by **migrate** if it finds a failed migration, but Flyway validates the history before the application finishes starting, so a failed migration normally stops the startup first; in that case repair it by hand (see [Troubleshooting](#troubleshooting)).
- Flyway settings that matter: `validate-on-migrate=true` (an applied migration whose file changed stops the startup), `out-of-order=false` (a migration with a lower number than one already applied is not accepted), `ignore-future-migrations=true` (a database that is newer than the application is tolerated), `clean-on-validation-error=false` (never wipes data).

## Migration history

| Version | File | What it does |
|---|---|---|
| V1 | `V1__Initial_schema.sql` | Tables `users`, `api_configs`, `request_logs` and their indexes |
| V2 | `V2__Add_delay_ms_column.sql` | `api_configs.delay_ms` (response delay) |
| V3 | `V3__Add_delay_ms_column.sql` | Despite its name: adds `users.role` and `users.enabled`, and fills empty `delay_ms`, `role` and `enabled` values. The name is kept because renaming an applied migration breaks validation. |
| V4 | `V4__Add_description_and_note_columns.sql` | `api_configs.description` (500) and `note` (2000) |
| V5 | `V5__Create_app_settings.sql` | `app_settings`: key/value settings edited in Admin → Settings |
| V6 | `V6__Add_failure_simulation.sql` | `api_configs.failure_enabled`, `failure_count`, `failure_status_codes`, `retry_after_seconds` |
| V7 | `V7__Create_mcp_tokens.sql` | `mcp_tokens`: one MCP token per user, stored as a hash |
| V8 | `V8__Create_mcp_calls.sql` | `mcp_calls`: audit trail of MCP tool calls, with an index on `(user_id, created_at)` |

Tables that reference `users` use `ON DELETE CASCADE`, so deleting a user removes their configurations, logs, MCP token and MCP calls.

Settings stored in `app_settings` (managed from the UI, no migration needed to add a key): `general.*` (site name, public base URL, registration, log retention), `mcp.*` (enabled, rate limit, page size, extra origins) and `maintenance.*` (last run, compaction state).

## Upgrading

1. **Back up** (see [Backup and restore](#backup-and-restore)). Always, even for small upgrades.
2. **Stop** the old version (H2 allows only one process to open the file).
3. **Deploy** the new version: replace the JAR or pull the new image and keep the same `./data` volume. Do not copy an old `data` directory over a newer one.
4. **Start** it and read the log. A healthy upgrade logs lines like:

   ```
   Successfully applied 1 migration to schema "PUBLIC", now at version v8
   Current database version: 8
   Database is up to date. All migrations have been applied.
   ```

5. **Check** that you can sign in and that your endpoints and logs are there. If anything looks wrong, stop and restore the backup ([Rolling back](#rolling-back)).

Skipping versions is safe: Flyway applies every missing migration in order.

## Backup and restore

**Offline backup (simplest, consistent):**

```bash
# stop the app first
cp ./data/mockwebhook.mv.db ./data/mockwebhook_backup_$(date +%Y%m%d_%H%M%S).mv.db
```

Docker: `docker-compose stop mockapi`, copy the file from the mounted `./data` directory, then `docker-compose start mockapi`.

**Online backup:** open the [H2 console](#using-the-h2-console) and run

```sql
BACKUP TO './data/backup.zip';
```

This writes a ZIP of the database files without stopping the application. Do not copy `mockwebhook.mv.db` while the application is running; the copy may be inconsistent.

**Restore:**

1. Stop the application.
2. Put the backup back as `./data/mockwebhook.mv.db` (unzip it first if it came from `BACKUP TO`). Delete a stale `mockwebhook.trace.db` if you like.
3. Start the application **of the same or a newer version** than the one that wrote the backup.

The database file contains password hashes, MCP token hashes and request logs. Store backups with the same care as the live file.

## Checking the migration state

**From the log:** look for `Current database version: N` and `Database is up to date`. `There are N pending migrations` or `Migration error` mean something needs attention.

**From SQL** (H2 console, or the H2 shell while the application is stopped):

```sql
SELECT "installed_rank", "version", "description", "success"
FROM "flyway_schema_history" ORDER BY "installed_rank";
```

The `"success"` column should be `TRUE` for every row. (The table and column names are lower case and quoted.)

**With the script:** `./validate_migration.sh` (application stopped) prints the migration history and checks the `delay_ms` column of `api_configs`. It was written for the first migrations and does not check the later ones, so use the SQL above for a full picture.

## Maintenance

The database is kept tidy by a **maintenance run**: it deletes request logs older than the retention you set, deletes MCP calls older than 90 days, then runs H2 `CHECKPOINT` (writes pending changes and trims the transaction log) and `ANALYZE` (refreshes query statistics).

- It runs **automatically every day at 03:30 (server time)** and on demand from **Admin → Settings → Database maintenance → Run Maintenance**. The page shows the database size, the time and the result of the last run.
- **Reclaiming disk space:** deleting rows does not shrink the file by itself. The first maintenance run on a database that was never compacted **schedules a compaction for the next restart** (the card shows "Compaction on next restart"). H2 can only compact safely before the connection pool opens, so on the next start the application runs `SHUTDOWN COMPACT` and then continues starting. While it runs, nothing else can use the database (startup is slower: seconds for a small database, longer for a big one), and it needs **free disk space about equal to the database size**. If there is not enough space, the compaction is skipped, a warning is recorded in the card, and it is retried at the next start.
- A run that deletes a lot of data can make the file grow temporarily before the next compaction shrinks it; leave room on the disk.
- Request logs are the main thing that grows. Set **Request log retention (days)** in Admin → Settings → General to keep it bounded (the default 0 keeps everything).

## Writing a new migration

1. Check the highest version in `src/main/resources/db/migration/` **on `coding`** (and in open pull requests) and take the next number. Two branches must not use the same number; if another branch merged first, renumber yours.
2. Name it `V<next>__Short_description.sql`, in English.
3. Make it **idempotent**: `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`. Hibernate may already have created the column from the entity.
4. For columns added to existing tables, give a **default** (or fill existing rows) so old rows are valid, and keep the entity field in step (same name, length and default).
5. Foreign keys to `users` should use `ON DELETE CASCADE`: users are deleted through JPA, which does not know about child tables that have no entity mapping.
6. **Never edit a migration that has been applied anywhere** (merged to `coding` or released). Flyway stores a checksum and refuses to start if it changes. Fix mistakes with a new migration.
7. Try it on a **copy of a real database** (an old one if possible), not only on an empty one, and run the tests (`./mvnw test`).
8. Use `out-of-order=false` in mind: a migration numbered lower than one that is already applied on some database will be rejected there.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Migration checksum mismatch` / `Validate failed` at startup | An applied migration file was edited. Restore the original file. If the change must stay (for example a comment fix), stop the app and run `./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=password` to realign the checksums. `./repair_database.sh` wraps a similar command. |
| `Detected resolved migration not applied to database` | A migration with a number lower than the latest applied one was added later. Renumber it above the latest version (see [Writing a new migration](#writing-a-new-migration)). |
| A migration is marked failed in `flyway_schema_history` (the application does not start) | Read the SQL error in the log and fix its cause (or restore a backup). With the application stopped, run `./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=password` to remove the failed entry, then start again so the migration is re-applied. |
| `Table already exists` or `Column already exists` | The migration is not idempotent. Use `IF NOT EXISTS` (the existing ones do). |
| `Database may be already in use` / locked | Another process has the file open (a running app, a stuck process, or the H2 console). Stop all of them, wait a few seconds, start again. |
| The application starts but a page fails with a missing table/column | The code is newer than the database (migrations did not run) or the other way round. Check `Current database version` in the log and the table above. |
| Everything is broken and there is no backup | Last resort: stop the app and delete the `./data` directory to start from an empty database. **This deletes all users, endpoints and logs.** A new administrator is created on start. |

The Flyway log may warn that "H2 2.1.214 is newer than this version of Flyway and support has not been tested"; it is expected with the current dependencies.

## Rolling back

There are **no down migrations**. To go back:

1. Stop the application.
2. Restore the backup taken before the upgrade ([Backup and restore](#backup-and-restore)).
3. Start the **previous version** of the application with it.

Do not run an older version against a database that a newer version has already upgraded: Flyway tolerates the unknown newer migrations (`ignore-future-migrations=true`) but the old code does not know the new columns and tables. To undo a change in a released schema, write a new migration instead of editing or deleting an old one.

## Using the H2 console

For inspection and manual fixes. It is **enabled by default** and is a security risk on a public server (see [SECURITY.md](SECURITY.md)); turn it off in production with `SPRING_H2_CONSOLE_ENABLED=false`.

1. Start the application and open `http://localhost:8081/h2-console`.
2. JDBC URL `jdbc:h2:file:./data/mockwebhook`, user `sa`, password `password` (or the one you set).
3. Take a backup before running any statement that changes data.

For a database file the application is not using, you can also use the H2 shell: `java -cp <h2.jar> org.h2.tools.Shell -url jdbc:h2:file:./data/mockwebhook -user sa -password password`.

## Scripts

| Script | Purpose | Notes |
|---|---|---|
| `validate_migration.sh` | Prints the migration history and checks the `delay_ms` column | Needs Maven and the `./data` database; application stopped. Covers the early migrations only. |
| `repair_database.sh` | Runs `flyway:repair` through Maven after a confirmation | Application stopped. It passes the Spring properties file to Flyway; if it cannot find the database, run the explicit `flyway:repair` command from [Troubleshooting](#troubleshooting). |
| `entrypoint.sh` | Optional container entrypoint that runs the Flyway CLI repair before starting | Not used by the default `Dockerfile` (it starts the JAR directly). |
