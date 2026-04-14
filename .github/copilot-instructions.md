Purpose

This file guides future Copilot CLI/code-assistant sessions for WebHookMock (Spring Boot Java 17).

Build / Test / Run (use Maven wrapper)

- Full build: ./mvnw clean install
- Package jar: ./mvnw package
- Run locally (dev): ./mvnw spring-boot:run
- Run Jar: java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar
- Run tests (all): ./mvnw test
- Run a single test class: ./mvnw -Dtest=MyTestClass test
- Run a single test method: ./mvnw -Dtest=MyTestClass#myMethod test
- Windows (PowerShell / cmd): use mvnw.cmd instead of ./mvnw
- Flyway repair (if needed): ./mvnw flyway:repair -Dflyway.url=jdbc:h2:file:./data/mockwebhook -Dflyway.user=sa -Dflyway.password=
- Docker compose: docker-compose up -d

CI

- GitHub Actions use .github/workflows/maven-test.yml and ci-cd.yml — they run: ./mvnw clean package and ./mvnw test, then build and publish a Docker image.

Where key configuration lives

- application properties: src/main/resources/application.properties (server.port=8081, H2 file DB at ./data/mockwebhook)
- Flyway migrations: src/main/resources/db/migration (add V{n}__Description.sql)
- Security and web config: src/main/java/vn/autobot/webhook/config (SecurityConfig.java, WebSocketConfig.java, WebConfig.java)

High-level architecture (big picture)

- Entry points
  - Web UI & admin pages: Thymeleaf templates in src/main/resources/templates
  - Public webhook endpoints: /api/@{username}/** handled by ApiMockController
  - Swagger UI: /swagger-ui and per-user specs at /swagger/@{username}

- Layers
  - Controller layer: src/main/java/vn/autobot/webhook/controller (ApiMockController, ApiConfigController, RequestLogController, AuthController, AdminController, WebhookViewController)
  - Service layer: src/main/java/vn/autobot/webhook/service (ApiMockService handles webhook processing and simulated delays; WebSocketService pushes real-time logs; ApiConfigService, RequestLogService, UserService, SwaggerService)
  - Persistence: src/main/java/vn/autobot/webhook/repository (Spring Data JPA repositories), entities in model/ and DTOs in dto/
  - Frontend assets: src/main/resources/static (js/css) and templates (Thymeleaf)
  - Real-time: STOMP WebSocket for live request logs (WebSocketConfig)

Key conventions and repo-specific patterns

- Webhook URL pattern: /api/@{username}/{custom-path} — these endpoints are public (SecurityConfig excludes /api/** from UI auth).
- Database: H2 file stored under ./data/mockwebhook by default. Keep migrations in src/main/resources/db/migration; follow Flyway V{n}__desc.sql naming.
- Response delay: database column and migrations support delay_ms; ApiMockService implements simulated delays (0-60000ms in UI/docs).
- Templates: reuse Thymeleaf fragments in templates/fragments; UI controllers expect these fragments.
- Passwords: BCryptPasswordEncoder used for UI accounts (check UserDetailsServiceImpl and SecurityConfig).
- Swagger: application serves static swagger.json and exposes UI at /swagger-ui.
- Using Maven wrapper: prefer ./mvnw (or mvnw.cmd on Windows) so CI and local runs match.

Files to check first when debugging

- src/main/resources/application.properties (runtime defaults)
- src/main/java/vn/autobot/webhook/config/SecurityConfig.java (path permissions, CSRF exemptions)
- src/main/java/vn/autobot/webhook/controller/ApiMockController.java (webhook routing)
- src/main/resources/db/migration (apply new schema changes here)

Other AI assistant / tooling configs

- No CLAUDE.md, AGENTS.md, .windsurfrules, .cursorrules, or other assistant config files were found. CI workflows are present in .github/workflows.

Notes for Copilot sessions

- Use the Maven wrapper commands above. For editing DB schema, add a Flyway migration under src/main/resources/db/migration and run the app; CI runs migrations automatically.
- When creating or modifying webhook endpoints, verify SecurityConfig and CSRF exemptions so test traffic is accepted.
- For quick local runs, Docker Compose (docker-compose.yml) starts the app with the same defaults; the H2 DB is persisted to ./data.

MCP servers

- Would you like to configure any MCP servers (e.g., Playwright) for this project? Reply yes to proceed or no to skip.

Summary

- Created repository-specific Copilot instructions: build/test commands (including single-test examples), high-level architecture, conventions, and pointers to important files. If any area needs more coverage (for example, testing patterns, integration test setup, or CI secrets), say which and it will be added.
