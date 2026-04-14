# AGENTS.md - WebHookMock

## Dev Commands

| Command | Description |
|---------|-------------|
| `./mvnw spring-boot:run` | Run application (Linux/macOS) |
| `mvnw.cmd spring-boot:run` | Run application (Windows) |
| `./mvnw test` | Run unit tests |
| `./mvnw clean package` | Build JAR to `target/` |
| `./validate_migration.sh` | Validate Flyway migration |
| `./repair_database.sh` | Repair failed migrations |

## Run Single Test
```bash
./mvnw test -Dtest=ClassName#methodName
```

## Architecture

- **Entry point**: `vn.autobot.webhook.WebHookMockApplication`
- **Path**: `/api/@{username}/{endpoint}` for user webhooks
- **Default port**: 8081
- **Database**: H2 file-based at `./data/mockwebhook`

## Test Order

`clean package` builds first, then `test` runs. No separate lint step (Maven compiles with warnings).

## Database Migration Issues

If Flyway migration fails:
1. Run `./repair_database.sh`
2. If persists, delete `./data/` directory (fresh start)

## Key Files

- Controllers: `src/main/java/vn/autobot/webhook/controller/`
- Services: `src/main/java/vn/autobot/webhook/service/`
- Models: `src/main/java/vn/autobot/webhook/model/`
- Flyway migrations: Check build config in `pom.xml`

## Docker

```bash
docker-compose up -d  # Runs on port 8081
```