# Database Guide

How WebHookMock stores its data, how the schema is upgraded, and how to back up, restore, inspect and maintain the database.

## Contents

- [How it works](#how-it-works)
- [Migration history](#migration-history)
- [Upgrading](#upgrading)
- [Backup and restore](#backup-and-restore)
- [Inspecting the database](#inspecting-the-database)
- [Checking the migration state](#checking-the-migration-state)
- [Maintenance](#maintenance)
- [Writing a new migration](#writing-a-new-migration)
- [Troubleshooting](#troubleshooting)
- [Rolling back](#rolling-back)
- [Scripts](#scripts)

## How it works

- The database is an **H2 2.1 file database** at `./data/mockwebhook` (`mockwebhook.mv.db`, plus `mockwebhook.trace.db` for H2 diagnostics). With Docker, `./data` is a mounted volume.
- **Only one process can open the file.** The application has no web console, so to read or change data you stop it and use the H2 shell ([Inspecting the database](#inspecting-the-database)).
- Schema changes are **Flyway migrations** in `src/main/resources/db/migration/` (`V<number>__<Description>.sql`). They run at startup, before the application uses the database.
- **Hibernate also updates the schema** (`spring.jpa.hibernate.ddl-auto=update`): after Flyway, it adds missing columns and tables that an entity needs. It never drops anything. Because Hibernate may have created a column first, **migrations must be idempotent** (`IF NOT EXISTS`).
- Databases from before Flyway are handled by `baseline-on-migrate=true` with `baseline-version=1`: a database without history is treated as version 1, and only later migrations run.
- `DatabaseMigrationConfig` logs the migration state after startup. Flyway validates the history first, so a failed migration normally stops the startup; repair it by hand ([Troubleshooting](#troubleshooting)).
- Flyway settings that matter: `validate-on-migrate=true` (an edited applied migration stops the startup), `out-of-order=false` (a migration numbered below an applied one is rejected), `ignore-future-migrations=true` (a newer database is tolerated), `clean-on-validation-error=false` (data is never wiped).

## Migration history

| Version | File | What it does |
|---|---|---|
| V1 | `V1__Initial_schema.sql` | Tables `users`, `api_configs`, `request_logs` and their indexes |
| V2 | `V2__Add_delay_ms_column.sql` | `api_configs.delay_ms` |
| V3 | `V3__Add_delay_ms_column.sql` | Despite its name: `users.role` and `users.enabled`, and fills empty `delay_ms`, `role` and `enabled`. Renaming an applied migration would break validation, so the name stays. |
| V4 | `V4__Add_description_and_note_columns.sql` | `api_configs.description` (500) and `note` (2000) |
| V5 | `V5__Create_app_settings.sql` | `app_settings`: key/value settings from Admin → Settings |
| V6 | `V6__Add_failure_simulation.sql` | `api_configs.failure_enabled`, `failure_count`, `failure_status_codes`, `retry_after_seconds` |
| V7 | `V7__Create_mcp_tokens.sql` | `mcp_tokens`: one MCP token hash per user |
| V8 | `V8__Create_mcp_calls.sql` | `mcp_calls`: audit trail of MCP tool calls, indexed on `(user_id, created_at)` |

Release 1.0.12 added no migration; its security changes (masked request logs, validated restores) apply to data written from then on.

Tables that reference `users` use `ON DELETE CASCADE`: deleting a user removes their endpoints, logs, MCP token and MCP calls.

`app_settings` keys (no migration needed for a new key): `general.*` (site name, public base URL, registration, log retention), `mcp.*` (enabled, rate limit, page size, extra origins) and `maintenance.*` (last run, compaction state).

## Upgrading

1. **Back up** ([Backup and restore](#backup-and-restore)), even for small upgrades.
2. **Stop** the old version.
3. **Deploy** the new one: replace the JAR, or set the new image tag and keep the same `./data` volume. Never copy an old `data` directory over a newer one.
4. **Start** it and read the log. A healthy upgrade shows:

   ```
   Current database version: 8
   Database is up to date. All migrations have been applied.
   ```

   (`Successfully applied N migration(s)` appears when the release brings new migrations.)
5. **Check** that you can sign in and that your endpoints and logs are there. If not, stop and [roll back](#rolling-back).

Skipping versions is safe: Flyway applies every missing migration in order.

## Backup and restore

**Backup** (the application must be stopped; a copy taken while it runs may be inconsistent):

```bash
docker-compose stop mockapi          # or stop the JAR
cp ./data/mockwebhook.mv.db ./data/mockwebhook_backup_$(date +%Y%m%d_%H%M%S).mv.db
docker-compose start mockapi
```

There is no online backup: the application has no database console and H2 lets one process open the file. If you need backups without downtime, schedule this during a quiet period.

**Restore:**

1. Stop the application.
2. Put the backup back as `./data/mockwebhook.mv.db`; delete a stale `mockwebhook.trace.db` if present.
3. Start a version **the same as or newer than** the one that wrote the backup.

The file contains password hashes, MCP token hashes and request logs. Keep backups private, and delete old ones you no longer need: a backup taken before 1.0.12 can contain unmasked credentials from request headers.

## Inspecting the database

1. Stop the application and take a backup.
2. Get the H2 JAR of the same version as the application (2.1.214), for example from Maven Central: `https://repo1.maven.org/maven2/com/h2database/h2/2.1.214/h2-2.1.214.jar`.
3. Open the shell:

   ```bash
   java -cp h2-2.1.214.jar org.h2.tools.Shell \
        -url jdbc:h2:file:./data/mockwebhook -user sa -password '<password>'
   ```

4. Run your statements, type `exit`, and start the application again.

Statements that change data have no undo; run them on a backup first. Common ones:

```sql
-- Change the database password (then set SPRING_DATASOURCE_PASSWORD to the same value)
ALTER USER SA SET PASSWORD '<new password>';

-- Delete every request log, or those before a date
DELETE FROM request_logs;
DELETE FROM request_logs WHERE timestamp < TIMESTAMP '2026-01-01 00:00:00';

-- Shrink the file after large deletions (closes the database)
SHUTDOWN COMPACT;
```

To delete request logs without the shell, use a retention period in Admin → Settings → General and **Run Maintenance** ([Maintenance](#maintenance)). Deleting a user in Admin → Users also deletes their data.

## Checking the migration state

**From the log:** `Current database version: N` and `Database is up to date` are good; `There are N pending migrations` or `Migration error` need attention.

**From SQL** (H2 shell):

```sql
SELECT "installed_rank", "version", "description", "success"
FROM "flyway_schema_history" ORDER BY "installed_rank";
```

Every row should have `"success" = TRUE`. The table and column names are lower case and must be quoted.

**With the script:** `./validate_migration.sh` (application stopped) prints the history and checks one early column; use the SQL above for the full picture.

## Maintenance

A **maintenance run** deletes request logs older than the retention you set, deletes MCP calls older than 90 days, then runs H2 `CHECKPOINT` (writes pending changes, trims the transaction log) and `ANALYZE` (refreshes query statistics).

- It runs **every day at 03:30 UTC (the application always runs in UTC)** and on demand from **Admin → Settings → Database maintenance → Run Maintenance**. The card shows the database size and the last result.
- **Disk space is not reclaimed by deleting rows.** The first run on a database that was never compacted **schedules a compaction for the next restart** ("Compaction on next restart"). On that start the application runs `SHUTDOWN COMPACT` before opening the connection pool, then continues. Startup takes longer (seconds for a small database), and it needs **free disk space about equal to the database size**; without it the compaction is skipped, a warning appears in the card, and it is retried on the next start.
- A large deletion can make the file grow for a while before the compaction shrinks it.
- Request logs are what grows. Set **Request log retention (days)**; the default 0 keeps everything.

## Writing a new migration

1. Take the next number after the highest version on `coding` and in open pull requests. If another branch merged the same number first, renumber yours.
2. Name it `V<next>__Short_description.sql`, in English.
3. Make it **idempotent**: `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`.
4. Give new columns on existing tables a **default** (or fill old rows), and keep the entity field in step (name, length, default).
5. Foreign keys to `users` use `ON DELETE CASCADE`.
6. **Never edit a migration that has been applied anywhere** (merged to `coding` or released): Flyway checks a checksum. Fix mistakes with a new migration.
7. Test on a **copy of a real database**, not only an empty one, and run `./mvnw test`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Migration checksum mismatch` / `Validate failed` | An applied migration was edited. Restore the original file. If the change must stay, stop the application and run `./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=<password>`. |
| `Detected resolved migration not applied to database` | A migration numbered below the latest applied one was added. Renumber it above the latest version. |
| A migration is marked failed and the application does not start | Read the SQL error in the log and fix the cause (or restore a backup). With the application stopped, run the `flyway:repair` command above to remove the failed entry, then start again. |
| `Table already exists` / `Column already exists` | The migration is not idempotent; use `IF NOT EXISTS`. |
| `Database may be already in use` | Another process has the file open: a second instance, a stuck process or an H2 shell. Stop them all, wait a few seconds, start again. |
| A page fails with a missing table or column | The code and the database are out of step. Compare `Current database version` in the log with the table above. |
| Startup is slow once after maintenance | The scheduled compaction is running; let it finish. |
| Nothing works and there is no backup | Last resort: stop the application and delete `./data`. **This deletes all users, endpoints and logs.** A new `admin` is created on start. |

Flyway may warn that "H2 2.1.214 is newer than this version of Flyway"; this is expected with the current dependencies.

## Rolling back

There are **no down migrations**. To go back:

1. Stop the application.
2. Restore the backup taken before the upgrade.
3. Start the **previous version** with it.

Do not run an older version against a database a newer version has upgraded: Flyway tolerates the unknown migrations, but the old code does not know the new columns. Rolling back below 1.0.12 also brings back its security problems ([SECURITY.md](SECURITY.md#supported-versions)).

## Scripts

| Script | Purpose | Notes |
|---|---|---|
| `validate_migration.sh` | Prints the migration history | Needs Maven and `./data`; application stopped. Checks early migrations only. |
| `repair_database.sh` | Runs `flyway:repair` through Maven after a confirmation | Application stopped. If it cannot find the database, use the explicit command in [Troubleshooting](#troubleshooting). |
| `entrypoint.sh` | Optional container entrypoint that runs a Flyway repair before starting | Not used by the default `Dockerfile`. |
