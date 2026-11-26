[![Java CI/CD with Maven and Docker](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/nguyenquy0710/Webhook-MockAPI/actions/workflows/ci-cd.yml)

# WebHookMock

A mock server for webhooks and REST APIs. Create endpoints that return the response you choose, point your application or a third-party service at them, and watch every request arrive in real time. AI clients can manage the same endpoints through the built-in [MCP server](#mcp-server).

Every user gets a namespace: `https://<your-domain>/api/@<username>/<path>`.

## Contents

- [Features](#features)
- [Quick start](#quick-start)
- [Using WebHookMock](#using-webhookmock)
- [MCP server](#mcp-server)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Upgrading](#upgrading)
- [Administration](#administration)
- [Development](#development)
- [Project structure](#project-structure)
- [Security](#security)
- [Documentation](#documentation)

## Features

**Mock endpoints**

- Endpoints per user, defined by **method + path**, with status code, content type, response body and response headers.
- **Response templates** in the body: `{{status}}`, `{{method}}`, `{{path}}`, `{{remoteAddr}}`, `{{headers.User-Agent}}`, `{{queryParams.utm_source}}`, `{{params.id}}`, `{{cookies.name}}`.
- **Response delay** of 0 to 60,000 ms to simulate latency.
- **Failure simulation** for testing retry logic: the first N calls of each caller get error codes from your list (400–599), then the configured response. Callers are told apart by `Idempotency-Key`, else `X-Request-Id`, else their IP; 429 and 503 carry `Retry-After`.
- **Description and note** per endpoint; the description becomes the summary in the endpoint's Swagger spec.
- **Backup and restore** of all your endpoints as JSON. A restore is checked first and saves nothing if one item is invalid.
- **Swagger**: your own OpenAPI spec at `/swagger/@<username>` (only you can see it) and general documentation at `/swagger-ui`.

**Request monitoring**

- Every call to your endpoints is logged: method, path, query, headers, body, client IP, response status and body, and a `curl` command.
- Values of cookies and of sensitive-looking headers and query parameters (authorization, cookie, token, password, secret, API key) are stored as `***`. Request bodies are stored as received.
- **Live updates** over WebSocket; filters by path, method and status class; paging; details panel; Excel export.
- Dashboard with today's request count, the latest requests and your webhook URL.

**Accounts and administration**

- Registration (can be switched off), login, password change. Passwords need 8 characters and must not be a common password or the username.
- Roles `USER` and `ADMIN`. Each user sees only their own endpoints, logs and Swagger spec.
- Admin area: users, and **Settings** (site name, public base URL, registration, request log retention, MCP options, database maintenance).

## Quick start

Requirements: **Java 17**, and Maven 3.6+ or the included `./mvnw`.

```bash
git clone https://github.com/nguyenquy0710/Webhook-MockAPI.git
cd Webhook-MockAPI
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Open <http://localhost:8081>. On the first start an administrator is created: `admin` / `admin123`. **Change that password at once** (user menu → Change Password).

Other ways to run it:

```bash
# JAR
./mvnw clean package
java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar

# Docker (port 8081, ./data and ./logs mounted)
docker-compose up -d
```

Images are published to `ghcr.io/nguyenquy0710/mockapi`. Use an explicit version tag rather than `latest` (see [Upgrading](#upgrading)).

## Using WebHookMock

1. **Register**, or sign in as `admin`. The dashboard shows your base URL, `https://<domain>/api/@<username>`.
2. Open **API Configs** → **New API**, enter a path such as `/orders`, and choose the method, status and body.
3. **Call it**: `curl -X POST https://<domain>/api/@<username>/orders -d '{"id":1}'`. Calling an endpoint needs no login; that is what makes it usable as a webhook target.
4. Open **Request Logs** to see the call; click a row for the details and the `curl` command.

| Area | URL | Who |
|---|---|---|
| Mock endpoints | `/api/@<username>/<path>` | Anyone |
| Dashboard | `/dashboard` | Signed-in user |
| API configurations | `/api-configs` | Signed-in user |
| Request logs | `/view/@<username>` | That user |
| Swagger spec of a user | `/swagger/@<username>` | That user |
| General API docs | `/swagger-ui`, `/swagger.json` | Anyone |
| MCP | `/mcp/@<username>/mcp`, `/mcp/@<username>/sse` | Holder of that user's MCP token |
| MCP activity | `/mcp-activity` | Signed-in user |
| Admin | `/admin/users`, `/admin/settings`, `/admin/user/@<username>/logs` | Administrators |

Times are shown in UTC.

**Things that behave differently from a plain web server**

- Mock responses carry `Content-Security-Policy: sandbox`: an HTML mock opened in a browser shows its content but runs no scripts or forms. `Set-Cookie` and `Clear-Site-Data` in your response headers are dropped. Both protect the signed-in pages, which share the host. API clients are not affected.
- Mock endpoints answer cross-origin browser calls without credentials (`Access-Control-Allow-Origin: *`); a browser call made with `credentials: 'include'` fails.
- A `curl` command copied from the log contains `***` instead of credentials, so replace them before running it.
- Unknown users and unknown paths get the same answer: `404 API endpoint not configured for this path and method`.

**Calling the JSON endpoints from a script**

`/api/logs/@<username>` and `/api/configs/...` use the browser session. Without one they answer `401`. Requests that change data (POST, DELETE) also need the CSRF token: read it from `<meta name="_csrf">` in any page and send it as `X-CSRF-TOKEN`. Page size is at most 200 on the HTML pages and 2,000 on `/api/logs`; sorting is limited to `id`, `timestamp`, `method`, `path` and `responseStatus`.

## MCP server

An [MCP](https://modelcontextprotocol.io) server runs inside the application, on the same port, so AI clients can create and update endpoints and read statistics and logs for you.

**Connect a client**

1. On the dashboard, open the **MCP Server** card and click **Generate Token**. The token (`mcp_...`) is shown once; only its hash is stored. **Renew Token** replaces it, and the old token stops working at once.
2. Point the client at `https://<domain>/mcp/@<username>/mcp` with `Authorization: Bearer <token>`. The card's **Connect a client** section has snippets for Claude Code, Cursor, cURL and SSE; check them against your client's documentation, since client formats change.

Clients that only speak the older HTTP+SSE transport can use `https://<domain>/mcp/@<username>/sse` with the same header.

| Tool | What it does |
|---|---|
| `list_api_configs` | Your endpoints, paged |
| `get_api_config` | One endpoint with its full configuration and `updatedAt` |
| `create_api_config` | Create an endpoint (validated; one method + path only once) |
| `update_api_config` | Change fields; pass `expectedUpdatedAt` to avoid overwriting newer edits |
| `list_request_logs` | Logs of one endpoint, paged, filterable by status class; sensitive values masked |
| `get_request_log_stats` | Totals by method, status class and day, and top paths, for a date range |

- A token acts as its owner and can use every tool. Keep it secret; renew it if it may have leaked.
- Every call is recorded with redacted arguments and shown at **MCP Activity**; entries are kept 90 days.
- In **Admin → Settings** administrators switch MCP on or off and set the rate limit per token (default 60 requests a minute), the largest page size (default 50) and extra browser origins.
- Only Bearer tokens are supported; clients that require OAuth cannot connect. A user can hold 5 SSE sessions, the server 200.
- Behind a reverse proxy, turn off response buffering for `/mcp/` (see [Deployment](#deployment)).

## Configuration

Defaults live in `src/main/resources/application.properties`. Any property can be set by an environment variable (Spring relaxed binding: `app.domain` → `APP_DOMAIN`).

| Property | Environment variable | Default | Purpose |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8081` | HTTP port |
| `app.domain` | `APP_DOMAIN` | `https://mock-api.quyit.id.vn` | Public base URL used in webhook URLs; Admin → Settings overrides it |
| `server.tomcat.remoteip.remote-ip-header` | `SERVER_TOMCAT_REMOTEIP_REMOTE_IP_HEADER` | `X-Forwarded-For` | Header with the client IP (see [Deployment](#deployment)) |
| `server.tomcat.remoteip.internal-proxies` | `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES` | private and loopback ranges | Regular expression of the proxy addresses whose headers are trusted |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./data/mockwebhook` | Database file |
| `spring.datasource.username` / `password` | `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `sa` / `password` | Database credentials; see [SECURITY.md](SECURITY.md) before changing them |
| `logging.file.name` | `LOGGING_FILE_NAME` | `logs/app.log` | Log file |
| `JAVA_OPTS` | `JAVA_OPTS` | `-Xms256m -Xmx512m ...` (Docker) | JVM options |

Settings that change at runtime (site name, public base URL, registration, request log retention, MCP) are stored in the database and edited in **Admin → Settings**. The WebSocket origin is read from the public base URL at startup, so restart after changing it.

The database is an H2 file under `./data`; the schema is upgraded automatically at startup. See [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md).

## Deployment

Run it behind a reverse proxy that terminates HTTPS. The proxy must:

- **redirect HTTP to HTTPS.** The session cookie is marked `Secure` on HTTPS requests, so signing in over plain HTTP does not work;
- send `X-Forwarded-Proto` and the client IP. The application trusts these headers only when the proxy connects from a private or loopback address (otherwise set `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`). It uses the last address the proxy added to `X-Forwarded-For`, or a header of your choice if the proxy **overwrites** one;
- allow WebSocket upgrades, and not buffer `/mcp/`.

Then set the public base URL (`APP_DOMAIN` or Admin → Settings) to the `https://` address users open, and mount `./data` and `./logs` as volumes.

Nginx (replace the host and certificate paths):

```nginx
server {
    listen 80;
    server_name mock-api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name mock-api.example.com;
    ssl_certificate     /etc/letsencrypt/live/mock-api.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mock-api.example.com/privkey.pem;
    client_max_body_size 10m;

    location / {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;   # define with a `map` block
    }

    location /mcp/ {                                        # MCP over SSE: keep the stream open
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }
}
```

HAProxy, the parts that matter:

```haproxy
frontend web
    http-request redirect scheme https code 301 unless { ssl_fc }
    http-request set-header X-Forwarded-Proto %[ssl_fc,iif(https,http)]
    http-request set-header X-Client-IP %[src]       # then SERVER_TOMCAT_REMOTEIP_REMOTE_IP_HEADER=X-Client-IP
```

If another proxy or CDN sits in front, put the HTTPS redirect there. After deploying, check that the **Source IP** of new request logs shows real client addresses: login limits and failure simulation depend on it.

## Upgrading

1. Stop the application and back up `./data` ([DATABASE_MIGRATION.md](DATABASE_MIGRATION.md#backup-and-restore)).
2. Deploy the new version with an **explicit image tag** (see the release notes); keep the same `./data` volume.
3. Start it and check the log for `Database is up to date`, then sign in.

Read the release notes before upgrading; [CHANGELOG.md](CHANGELOG.md) lists every release. **Versions before 1.0.12 have known security problems**, upgrade them.

## Administration

The first administrator, `admin` / `admin123`, is created on first start. Administrators can:

- create, enable or disable, promote or demote and delete users (**Admin → Users**), and read any user's request logs from there;
- change settings, MCP options and run database maintenance (**Admin → Settings**).

**Request log retention** (Settings → General) deletes logs older than N days in the daily maintenance run at 03:30 UTC; 0 (the default) keeps everything.

## Development

| Command | Purpose |
|---|---|
| `./mvnw spring-boot:run` | Run the application |
| `./mvnw test` | Run the tests |
| `./mvnw test -Dtest='ClassName*'` | Run one test class (the `*` also runs its nested classes) |
| `./mvnw clean package` | Build `target/WebHookMock-0.0.1-SNAPSHOT.jar` |

Use **Java 17**. Tests run against in-memory H2 databases; the security tests start the application on a random port.

New work goes on a `feature/*` branch from `coding`; pull before you start, and open pull requests against `coding`. `main` receives releases. UI standards are in [docs/specs/ui-ux.md](docs/specs/ui-ux.md) and a clickable prototype is in `docs/prototype/`. See also [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md).

## Project structure

```
src/main/java/vn/autobot/webhook/
  config/       Security, CORS, WebSocket, rate limits, startup compaction
  controller/   Pages, JSON endpoints, mock endpoints, MCP
  dto/          Request and response objects
  model/        JPA entities
  repository/   Spring Data repositories
  service/      Mock responses, logs, settings, users, MCP, maintenance
  utils/        Request helpers
src/main/resources/
  db/migration/ Flyway migrations
  templates/    Thymeleaf pages (error/ holds the 403 and 429 pages)
  static/       CSS, JavaScript, images
docs/           Guides, specs and the UI prototype
plans/          Plans and work logs
```

## Security

Read [SECURITY.md](SECURITY.md) before you expose an instance. It explains how to report a vulnerability, what is protected and how, the hardening checklist and the known limitations.

## Documentation

- [SECURITY.md](SECURITY.md): reporting, security model, hardening
- [DATABASE_MIGRATION.md](DATABASE_MIGRATION.md): schema history, upgrades, backup and restore, maintenance
- [CHANGELOG.md](CHANGELOG.md): release notes
- Guides in `docs/` ([installation](docs/INSTALLATION.md), [user guide](docs/USER-GUIDE.md), [architecture](docs/ARCHITECTURE.md), [deployment](docs/DEPLOYMENT.md), [testing](docs/TESTING.md), [contributing](docs/CONTRIBUTING.md)). Some predate the latest changes; where they differ, this README is right.

## Authors

- nchuyen128@gmail.com, <https://autobot.site>
- quyit.job@gmail.com, <https://blogs.nhquydev.net>

Issues and pull requests are welcome.
