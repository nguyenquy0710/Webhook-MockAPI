[![Java CI/CD with Maven and Docker](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml)

# WebHookMock

A mock server for webhooks and REST APIs. Create endpoints with the response you want, point your application (or a third-party service) at them, and watch every request arrive in real time. AI clients can manage the same endpoints through a built-in [MCP server](#mcp-server).

Every user gets a namespace: `https://<your-domain>/api/@<username>/<path>`.

## Contents

- [Features](#features)
- [Quick start](#quick-start)
- [Using WebHookMock](#using-webhookmock)
- [MCP server](#mcp-server)
- [Configuration](#configuration)
- [Administration](#administration)
- [Deployment](#deployment)
- [Development](#development)
- [Project structure](#project-structure)
- [Security](#security)
- [Documentation](#documentation)

## Features

### API configuration

- Endpoints per user, defined by **method + path**, with status code, content type, response body and response headers.
- **Response templates**: use request values in the body or headers, for example `{{headers.User-Agent}}`, `{{queryParams.utm_source}}`, `{{status}}`.
- **Response delay** (0–60000 ms) to simulate latency.
- **Description and note** per endpoint; the description becomes the summary in the generated Swagger spec.
- **Failure simulation** (for testing a client's retry logic): the first N calls of each caller (identified by the `Idempotency-Key` header, else `X-Request-Id`, else the client IP) get an error status, used in turn from your list (400–599), then the configured response; 429 and 503 carry `Retry-After`. Counters live in memory and reset after 10 idle minutes.
- **Backup and restore** of all your configurations as JSON.
- **Swagger**: per-user OpenAPI spec at `/swagger/@<username>` and Swagger UI at `/swagger-ui`.

### Request monitoring

- Every request to your endpoints is logged: method, path, query, headers, body, source IP, response status and body, plus a ready-to-run `curl` command.
- **Live updates** over WebSocket (STOMP).
- Filter by path, method and status class; paging; details panel; export to Excel.
- Dashboard with today's request count, the latest requests and your webhook URL.

### MCP server

An [MCP](https://modelcontextprotocol.io) server on the same port lets AI clients create and update endpoints and read statistics and request logs on your behalf. See [MCP server](#mcp-server).

### Accounts and administration

- Registration (can be switched off), login, password change; BCrypt password hashing.
- Roles `USER` and `ADMIN`; each user only sees their own endpoints and logs.
- Admin area: user management and **Settings** (site name, public base URL, log retention, registration, MCP options, database maintenance).

## Quick start

### Requirements

- Java 17
- Maven 3.6+ (or the included `./mvnw` wrapper)

### Run from source

```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Open <http://localhost:8081>. On the first start an administrator is created: user `admin`, password `admin123`. **Change it immediately** (user menu → Change Password) and read [SECURITY.md](SECURITY.md) before exposing the app.

### Run the JAR

```bash
./mvnw clean package
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar
```

### Run with Docker

```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
docker-compose up -d
```

The image is published as `ghcr.io/nguyenquy0710/mockapi:latest`. `docker-compose.yml` maps port 8081 and mounts `./data` (database) and `./logs`. Set `APP_DOMAIN` to the public URL of your deployment.

## Using WebHookMock

1. **Register** (or sign in as `admin`). The dashboard shows your webhook base URL, `https://<domain>/api/@<username>`.
2. Open **API Configs**, click **New API**, enter a path (for example `/orders`), choose the method, status and body.
3. **Call it**: `curl -X POST https://<domain>/api/@<username>/orders -d '{"id":1}'`. No authentication is needed to call your endpoints; that is what makes them usable as webhook targets.
4. Open **Request Logs** to see the request arrive; click a row for the full details and the `curl` command.

| Area | URL |
|---|---|
| Your mock endpoints | `/api/@<username>/<path>` |
| Dashboard | `/dashboard` |
| API configurations | `/api-configs` |
| Request logs | `/view/@<username>` |
| Swagger UI / per-user spec | `/swagger-ui`, `/swagger/@<username>` |
| MCP (Streamable HTTP / SSE) | `/mcp/@<username>/mcp`, `/mcp/@<username>/sse` |
| MCP activity | `/mcp-activity` |
| Admin | `/admin/users`, `/admin/settings` |

Times are shown in UTC.

## MCP server

The MCP server runs inside the app; it needs no extra port or process.

**Connect a client**

1. On the dashboard, find the **MCP Server** card and click **Generate Token**. The token (`mcp_...`) is shown once; only its hash is stored. **Renew Token** replaces it and the old token stops working immediately.
2. Point your client at `https://<domain>/mcp/@<username>/mcp` with the header `Authorization: Bearer <token>`. The card's **Connect a client** section has ready-made snippets (Claude Code, Cursor, cURL, SSE); check them against your client's documentation, as client configuration formats change.

Older clients that only speak HTTP+SSE can use `https://<domain>/mcp/@<username>/sse` with the same header.

**Tools**

| Tool | What it does |
|---|---|
| `list_api_configs` | Your endpoints, paged |
| `get_api_config` | One endpoint with its full configuration and `updatedAt` |
| `create_api_config` | Create an endpoint (validated; the same method and path cannot exist twice) |
| `update_api_config` | Change fields; pass `expectedUpdatedAt` to avoid overwriting newer edits |
| `list_request_logs` | Logs of one endpoint, paged, filterable by status class; sensitive headers masked |
| `get_request_log_stats` | Totals by method, status class and day, and top paths for a date range |

**Good to know**

- A token acts as its owner and can use every tool; keep it secret and renew it if it leaks.
- Every tool call is recorded (arguments redacted) and shown at **Dashboard → Activity**; entries are kept 90 days.
- Admins control MCP in **Admin → Settings**: on/off, rate limit per token (default 60 requests/minute), maximum page size and extra allowed browser origins.
- Clients that only support OAuth cannot connect; only Bearer tokens are supported.
- Tested with MCP Inspector 2.13.0 over both transports.
- Behind a reverse proxy, disable response buffering for `/mcp/` so the SSE transport works (see [Deployment](#deployment)).

## Configuration

Defaults are in `src/main/resources/application.properties`. Any property can be overridden with an environment variable (Spring relaxed binding), for example `app.domain` → `APP_DOMAIN`.

| Property | Environment variable | Default | Purpose |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8081` | HTTP port |
| `app.domain` | `APP_DOMAIN` | `https://mock-api.quyit.id.vn` | Public base URL used to build webhook URLs (also editable in Admin → Settings) |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/mockwebhook` | Database location |
| `spring.datasource.username` / `password` | `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `sa` / `password` | Database credentials (see [SECURITY.md](SECURITY.md) before changing) |
| `spring.h2.console.enabled` | `SPRING_H2_CONSOLE_ENABLED` | `true` | H2 web console at `/h2-console`; **turn off in production** |
| `logging.file.name` | `LOGGING_FILE_NAME` | `logs/app.log` | Log file |
| `JAVA_OPTS` | `JAVA_OPTS` | `-Xms256m -Xmx512m ...` (Docker) | JVM options |

Settings that can change at runtime live in **Admin → Settings** and are stored in the database, so they survive restarts and upgrades. They override the defaults above where both exist (site name, public base URL, registration, request-log retention, MCP).

The database is an H2 file under `./data`. Schema changes are applied automatically at startup; see [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md) for upgrades, backups and maintenance.

## Administration

The first administrator is created on first start (`admin` / `admin123`). Admins can:

- create, enable/disable, promote/demote and delete users (**Admin → Users**);
- change general settings, MCP options and run database maintenance (**Admin → Settings**).

**Request log retention** (Settings → General) deletes request logs older than N days during the daily maintenance run (0 keeps everything, the default).

## Deployment

- Run behind a reverse proxy that terminates HTTPS, forwards `X-Forwarded-*` headers (the source IP in the logs comes from `X-Forwarded-For`) and allows WebSocket upgrades.
- Set the public base URL (`APP_DOMAIN` or Admin → Settings).
- Mount `./data` and `./logs` as volumes; back up `./data` (see [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md)).

Minimal Nginx example (replace the host and certificate paths):

```nginx
server {
    server_name mock-api.example.com;
    listen 443 ssl;
    ssl_certificate     /etc/letsencrypt/live/mock-api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mock-api.example.com/privkey.pem;

    client_max_body_size 10m;

    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;   # define with a `map` block, see Nginx docs
    }

    # MCP over SSE: do not buffer, keep the stream open
    location /mcp/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }

    # Do not expose the database console
    location /h2-console { return 404; }
}
```

## Development

| Command | Purpose |
|---|---|
| `./mvnw spring-boot:run` | Run the app |
| `./mvnw test` | Run the tests |
| `./mvnw test -Dtest=ClassName#method` | Run one test |
| `./mvnw clean package` | Build `target/WebHookMock-0.0.1-SNAPSHOT.jar` |
| `./validate_migration.sh` | Check the migration state (app must be stopped) |
| `./repair_database.sh` | Help with failed migrations (see [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md)) |

Build and test with **Java 17**. If you use SDKMAN: `sdk use java 17.0.14-ms`; otherwise set `JAVA_HOME` to a JDK 17 and put its `bin` first in `PATH`.

Conventions: new work goes on a `feature/*` branch created from `coding`, pull before you start, and pull requests target `coding`. UI design standards are in [docs/specs/ui-ux.md](docs/specs/ui-ux.md); a clickable prototype of the UI lives in `docs/prototype/`. Plans and work logs are in `plans/`. See also [AGENTS.md](AGENTS.md).

## Project structure

```
src/main/java/vn/autobot/webhook/
  config/       Security, web, WebSocket, global model, startup compaction
  controller/   MVC pages, REST endpoints, mock API, MCP
  dto/          Request/response objects
  model/        JPA entities
  repository/   Spring Data repositories
  service/      Business logic (mock responses, logs, settings, MCP, maintenance)
src/main/resources/
  db/migration/ Flyway migrations (V1 ...)
  templates/    Thymeleaf pages
  static/       CSS, JavaScript, images
docs/           Guides, specs and the UI prototype
plans/          Plans and work logs
```

## Security

Read [SECURITY.md](SECURITY.md) before exposing an instance: it lists how to report a vulnerability, the security model, a hardening checklist (default admin password, H2 console, database password, HTTPS) and the known limitations.

## Documentation

- [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md): schema history, upgrades, backup and restore, maintenance
- [SECURITY.md](SECURITY.md): policy, model and hardening
- [CHANGELOG.md](CHANGELOG.md): release notes
- Guides in `docs/` ([installation](docs/INSTALLATION.md), [user guide](docs/USER-GUIDE.md), [architecture](docs/ARCHITECTURE.md), [deployment](docs/DEPLOYMENT.md), [testing](docs/TESTING.md), [contributing](docs/CONTRIBUTING.md)); some of them predate the latest features, so prefer this README where they differ.

## Author

- Developed by nchuyen128@gmail.com from <https://autobot.site>
- Developed by quyit.job@gmail.com from <https://blogs.nhquydev.net>

Issues and pull requests are welcome.
