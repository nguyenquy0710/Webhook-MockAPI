# Security Policy

WebHookMock is a mock server: by design it accepts unauthenticated requests on `/api/@<username>/...` and stores what it receives. That makes a few security decisions important for anyone who runs an instance. This document explains how to report a problem, how the application protects data, what you must do before exposing an instance, and which weaknesses are known.

## Supported versions

Security fixes are made on the latest code (`coding` / `main`) and released with the next version. Older releases are not patched; please upgrade to the latest release first.

## Reporting a vulnerability

**Do not open a public issue or pull request for a vulnerability.**

Report it privately by email to **quyit.job@gmail.com** with the subject `[SECURITY] WebHookMock`. Please include:

- what you found and why it matters (impact),
- the version or commit, and how the app is deployed (Docker, JAR, behind a proxy),
- steps to reproduce, or a proof of concept,
- any suggested fix.

What to expect (targets, not guarantees; this is a small project):

- an acknowledgement within **7 days**;
- an assessment and a plan or fix within **30 days** for confirmed issues, sooner for critical ones;
- credit in the release notes if you want it, after the fix is released;
- coordinated disclosure: please give us a reasonable time to release a fix before publishing details.

If a report is declined (for example it is a known limitation listed below, or needs privileges an attacker would not have), we will say why.

**In scope:** the application code in this repository, its default configuration and the Docker image built from it.
**Out of scope:** vulnerabilities in third-party dependencies that are already public (report those upstream; tell us if an update is needed), social engineering, denial of service by sheer volume of traffic, and issues that require an already-compromised server.

## Security model

| Area | Who can access | How it is protected |
|---|---|---|
| `/api/@<username>/**` (your mock endpoints) | Anyone | Public by design so third parties can call them. Responses are sandboxed by a `Content-Security-Policy` header and cannot set or clear cookies (`Set-Cookie`, `Clear-Site-Data`). Do not put secrets in response bodies. Whatever is sent to an endpoint is logged. |
| `/mcp/@<username>/**` (MCP server) | Holders of that user's MCP token | `Authorization: Bearer mcp_...`; the token must belong to the `@username` in the path; the server can be switched off in Admin → Settings; per-token rate limit; browser `Origin` allow-list; every tool call is audited. |
| `/swagger-ui`, `/swagger/**`, `/swagger.json` | Anyone | Documentation of the mock endpoints only. |
| `/register`, `/login`, static files | Anyone | Registration can be switched off in Admin → Settings. |
| `/h2-console/**` | Role `ADMIN` | **Off by default.** When switched on it needs an administrator session as well as the console's own login, whose default database credentials are public. See the hardening checklist. |
| `/admin/**` | Role `ADMIN` | Spring Security role check. |
| Everything else (dashboard, API configs, logs, MCP activity) | Signed-in users | Form login, a server-side session and CSRF tokens; each user only sees their own data. |

Authentication and storage:

- Passwords are hashed with **BCrypt**. Minimum length is 6 characters (no other complexity rule).
- **MCP tokens** are random (32 bytes), shown once, and only their SHA-256 hash is stored. Renewing a token invalidates the old one at once.
- Request logs keep headers, bodies and source IPs of whatever was sent to your endpoints, with the values of cookies and of sensitive-looking headers and query parameters replaced by `***` before storage. Bodies are stored as received. MCP tools that return logs mask values of keys that look sensitive (authorization, cookie, token, password, secret, api key) and cut long bodies; the MCP activity log stores arguments redacted and truncated.
- The database is a local H2 file (`./data/mockwebhook.mv.db`). It contains password hashes, MCP token hashes and the request logs, so treat the file and its backups as sensitive.

## Hardening checklist for operators

Do these before the instance is reachable from the Internet:

1. **Change the default administrator password.** On first start the app creates `admin` / `admin123` and writes that password to the log (`logs/app.log`) as a warning. Change it at once, then remove or rotate old log files that contain it.
2. **Keep the H2 console off** (the default; do not set `SPRING_H2_CONSOLE_ENABLED=true`). Its "localhost only" restriction does not protect it behind a reverse proxy, because the proxy may connect from localhost and forwarded addresses can be spoofed. Block `/h2-console` at the proxy as well.
3. **Change the database password.** The default (`sa` / `password`) is in the repository. H2 stores the password in the database file, so changing the property alone is not enough: connect with the old credentials and run `ALTER USER SA SET PASSWORD '<new>';` while the app is stopped, then set `SPRING_DATASOURCE_PASSWORD`. Take a backup first.
4. **Serve over HTTPS** only (terminate TLS at a reverse proxy, redirect HTTP to HTTPS, and send `X-Forwarded-Proto`; the session cookie is `SameSite=Lax`, and `Secure` with HSTS on HTTPS requests) and set the public base URL (`APP_DOMAIN` or Admin → Settings) to the `https://` address.
5. **Restrict registration** if the instance is not meant to be open: switch off "Allow new user registration" in Admin → Settings.
6. **Limit what the proxy exposes**: block `/h2-console`, consider restricting `/admin/` to trusted addresses, and set sensible request size limits (`client_max_body_size`).
7. **Decide on log retention.** Request logs can contain personal data or secrets sent by callers. Set a retention period in Admin → Settings → General (the default keeps everything) and avoid sending real secrets to mock endpoints.
8. **MCP:** leave it switched off (Admin → Settings → MCP Server) if nobody uses it. Treat a token like a password: it can create and change your endpoints and read your request logs. Renew it if it may have leaked and check **MCP Activity** for unexpected calls. Keep the rate limit; add allowed origins only for clients that really run in a browser.
9. **Protect backups and `./data`**: file permissions, encryption at rest where available, and no public volume mounts.
10. **Keep up to date.** Run the latest release. Spring Boot 2.7.x, used here, is past its open-source support period, so plan an upgrade of the framework as well.

## Known limitations

These are known and listed so you can decide whether they matter for your deployment. They are not secret; fixing them is welcome as a pull request.

- **Default credentials:** the default admin user and database password described above.
- **WebSocket origin** is read from the public base URL at startup; restart after changing it in Admin → Settings.
- **No brute-force protection:** no login rate limiting or lockout, and no rate limit on the public mock endpoints (only MCP has one). Use the proxy or a firewall.
- **Session cookie and headers** use the framework defaults: no Content-Security-Policy, and pages may be framed by the same origin (needed by the H2 console).
- **Password policy** is only a minimum length of 6.
- **In-memory MCP state:** the MCP rate limiter and SSE sessions are kept in memory, so they reset on restart and are not shared between instances.
- **MCP token scope:** one token per user with access to every tool; there is no read-only token and no OAuth.
- **Sensitive data in logs:** request bodies, header names that do not look sensitive, and logs written before masking was introduced are stored as received.

## Dependencies and updates

Dependencies are managed through Maven (`pom.xml`). Updates are applied by pull requests; a dependency vulnerability that affects this application is a good reason to open one, or to email us if it is severe.
