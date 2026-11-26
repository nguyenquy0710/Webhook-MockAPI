# Security Policy

WebHookMock is a mock server. By design it accepts unauthenticated requests on `/api/@<username>/...` and stores what it receives, on the same host as the signed-in pages. This document explains how to report a problem, what the application protects and how, what to do before you expose an instance, and what is known to be weak.

## Supported versions

| Version | Supported |
|---|---|
| 1.0.12 and later | Yes |
| Earlier than 1.0.12 | **No.** They have known security problems, including an open database console, missing CSRF protection, credentialed CORS for any origin, and request logs readable by other users. Upgrade. |

Fixes are made on `coding`, merged to `main` and published with the next release.

## Reporting a vulnerability

**Do not open a public issue or pull request for a vulnerability.**

Email **quyit.job@gmail.com** with the subject `[SECURITY] WebHookMock`, and include:

- what you found and its impact;
- the version or commit, and how the application is deployed (Docker, JAR, behind which proxy);
- steps to reproduce or a proof of concept;
- a suggested fix, if you have one.

Targets, not guarantees, since this is a small project:

- acknowledgement within **7 days**;
- an assessment and a plan or fix within **30 days** for confirmed issues, sooner for critical ones;
- credit in the release notes after the fix, if you want it;
- coordinated disclosure: please allow time for a release before publishing details.

We will explain why if we decline a report, for example because it is a known limitation listed below.

**In scope:** the application code in this repository, its default configuration and the Docker images built from it.
**Out of scope:** already public vulnerabilities in third-party dependencies (report them upstream, and tell us if an update is needed), social engineering, denial of service by traffic volume, and issues that need an already compromised server.

Testing against an instance you do not run needs its owner's permission. Use your own instance where you can.

## Security model

| Area | Who | How it is protected |
|---|---|---|
| `/api/@<username>/**` (mock endpoints) | Anyone | Public by design. Responses carry `Content-Security-Policy: sandbox`, so content opened in a browser runs in an isolated origin; `Set-Cookie` and `Clear-Site-Data` from endpoint headers are dropped. CORS for any origin, without credentials. Unknown users and unknown paths get the same 404. |
| `/mcp/@<username>/**` | Holder of that user's MCP token | Bearer token that must belong to the user in the path; can be switched off; rate limit per token; browser `Origin` allow-list; every tool call audited. |
| `/view/@<username>`, `/swagger/@<username>`, `/api/logs/@<username>`, `/api/configs/...` | That user | Session, and an owner check in each handler. Administrators read other users' logs through `/admin/user/@<username>/logs`. |
| `/admin/**` | Role `ADMIN` | Role check. |
| Other pages (dashboard, API configs, MCP activity, password change) | Signed-in users | Form login and a server-side session. |
| `/login`, `/register`, `/swagger-ui`, `/swagger.json`, static files | Anyone | Rate limits on login and registration; registration can be switched off. |
| WebSocket (`/ws`) | Signed-in users | Only this site's origin; a user can only subscribe to their own topic (administrators to any) and nobody can publish. |

Sessions and requests:

- **CSRF tokens** are required for every request that changes data with the session cookie, including the JSON endpoints (`X-CSRF-TOKEN` header). Mock endpoints, MCP and the WebSocket endpoint are exempt: they do not act on the session or check the origin themselves.
- The session cookie is `HttpOnly`, `SameSite=Lax`, and `Secure` on HTTPS requests; HTTPS responses carry HSTS. Logout is a POST.
- **Client address and scheme** come from proxy headers only when the proxy connects from a private or loopback address. The proxy's host and port headers are ignored.
- **Login limits** (per minute, per client address): 30 attempts, 10 per username, 5 registrations. There is no account lockout, so nobody can lock another person out.
- Pages send `Referrer-Policy`, `Permissions-Policy`, `X-Frame-Options: SAMEORIGIN` and a report-only Content-Security-Policy. Scripts and styles from CDNs are pinned to a version with an integrity hash.
- Without a session, the JSON endpoints answer 401; missing and foreign records both answer 404; invalid sorting answers 400; page sizes are capped.

Data:

- Passwords are hashed with **BCrypt**. New passwords need 8 characters and must not be on a list of common passwords or equal the username. Changing a password requires the current one.
- **MCP tokens** are 32 random bytes, shown once, and stored as a SHA-256 hash; renewing one invalidates the old token at once.
- **Request logs** keep what callers send. Values of cookies and of headers and query parameters whose names look sensitive (authorization, cookie, token, password, secret, API key) are replaced by `***` **before** they are stored, also in the `curl` command and in templated response bodies. Request bodies are stored as received. MCP views and the MCP activity log are redacted as well.
- **Restoring a backup** validates every item and saves nothing when one is invalid.
- The application has **no database console**. The database is a local H2 file (`./data/mockwebhook.mv.db`) holding password hashes, MCP token hashes and request logs; treat it and its backups as sensitive.

## Hardening checklist

Before the instance is reachable from the Internet:

1. **Change the default administrator password.** The first start creates `admin` / `admin123` and writes it to the log as a warning; remove or rotate log files that contain it.
2. **Change the database password.** The default (`sa` / `password`) is public. H2 keeps the password in the database file, so with the application stopped, connect with the old credentials, run `ALTER USER SA SET PASSWORD '<new>';`, then set `SPRING_DATASOURCE_PASSWORD`. Back up first ([DATABASE_MIGRATION.md](DATABASE_MIGRATION.md#inspecting-the-database)).
3. **HTTPS only:** redirect HTTP to HTTPS at the outermost proxy, send `X-Forwarded-Proto`, and set the public base URL to the `https://` address users open.
4. **Client IP:** make sure the proxy adds or overwrites the client address and connects from a private or loopback address (or set `SERVER_TOMCAT_REMOTEIP_INTERNAL_PROXIES`). Then check that request logs show real addresses; otherwise all users share one login limit.
5. **Do not expose the database file or another H2 server.** Keep `./data` and backups private, with restrictive file permissions.
6. **Restrict registration** if the instance is not public (Admin → Settings).
7. **Limit what the proxy exposes:** consider restricting `/admin/` to trusted addresses, set a request size limit, and strip internal headers (host names, instance names) from responses.
8. **Set a request log retention** (Admin → Settings → General); the default keeps everything. Callers may send personal data or secrets in bodies.
9. **MCP:** switch it off if nobody uses it. A token can change endpoints and read logs: renew it if it may have leaked and check **MCP Activity**.
10. **Stay current:** deploy releases by explicit version tag, not `latest`. Spring Boot 2.7, used here, is past its open-source support; a framework upgrade is planned work.

## Known limitations

Listed so you can judge whether they matter to you. Pull requests are welcome.

- **Default credentials** for the administrator and the database, as above.
- **Content-Security-Policy is report-only** and allows inline scripts, because the pages still use inline scripts and handlers.
- **Limits are in memory and per instance:** login, registration and MCP limits reset on restart, are not shared between instances, and do not stop attempts spread over many addresses. No CAPTCHA, email verification or MFA. Mock endpoints have no rate limit; use the proxy.
- **Delayed mock responses** each hold a thread for up to 60 seconds; many slow calls can exhaust the server. Limit traffic at the proxy.
- **Masking is by name:** a secret in a header or parameter with an unusual name, or in a request body, is stored as sent. Logs written before 1.0.12 are not masked.
- **Passwords** are checked against a short list only, not against leaked-password databases; existing passwords are not rechecked.
- **WebSocket origin** is read from the public base URL at startup; restart after changing it.
- **MCP tokens** are all-or-nothing: no read-only token, no OAuth.

## Dependencies

Dependencies are managed with Maven (`pom.xml`) and updated through pull requests. A dependency vulnerability that affects WebHookMock is a good reason to open one, or to email us if it is severe.
