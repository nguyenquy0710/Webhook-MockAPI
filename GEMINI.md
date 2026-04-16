# GEMINI.md - WebHookMock Project Context

## Project Overview
**WebHookMock** is a Spring Boot-based webhook testing tool designed to simplify the development and testing of webhook-driven applications. It allows developers to create custom API endpoints, configure dynamic responses (status codes, headers, bodies, delays), and monitor incoming requests in real-time.

### Main Technologies
- **Framework:** Spring Boot 2.7.9 (Java 17)
- **Security:** Spring Security (Form-based auth for UI, public access for `/api/**`)
- **Database:** H2 (File-based, stored in `./data/mockwebhook`)
- **Migrations:** Flyway
- **Frontend:** Thymeleaf, Bootstrap 5, WebSocket (STOMP for real-time logs)
- **Utilities:** Apache POI (Excel export), Lombok, Swagger/OpenAPI

### Architecture
- **Controllers:** 
  - `ApiMockController`: Handles incoming webhook requests via `/api/@{username}/**`.
  - `ApiConfigController`: Manages user-defined API configurations.
  - `RequestLogController`: Manages and exports request logs.
- **Services:** 
  - `ApiMockService`: Core logic for processing webhooks and simulating delays.
  - `WebSocketService`: Pushes real-time updates to the dashboard.
- **Persistence:** Repository pattern using Spring Data JPA.

---

## Building and Running

### Prerequisites
- **Java:** JDK 17
- **Build Tool:** Maven 3.6+ (or use included `./mvnw`)

### Commands
- **Build:**
  ```bash
  ./mvnw clean install
  ```
- **Run (Development):**
  ```bash
  ./mvnw spring-boot:run
  ```
- **Run (JAR):**
  ```bash
  java -jar target/WebHookMock-0.0.1-SNAPSHOT.jar
  ```
- **Docker:**
  ```bash
  docker-compose up -d
  ```

### Configuration
- **Port:** Default is `8081`.
- **H2 Console:** Available at `/h2-console` (Username: `sa`, Password: `password`).
- **Data Directory:** Database files are stored in `./data/`.

---

## Development Conventions

### Webhook API Structure
- Webhook endpoints follow the pattern: `/api/@{username}/{custom-path}`.
- These endpoints are publicly accessible (configured in `SecurityConfig`).

### Database & Migrations
- **Flyway:** Always add new migrations to `src/main/resources/db/migration` using the `V<Number>__<Description>.sql` naming convention.
- **Entities:** Located in `vn.autobot.webhook.model`. Use Lombok annotations for boilerplate code.

### Real-time Logs
- The application uses WebSocket (STOMP) to update the dashboard. Configuration is in `WebSocketConfig.java`.

### Security
- UI routes (dashboard, config, etc.) require authentication.
- Password encryption uses `BCryptPasswordEncoder`.

### Testing
- Run tests using `./mvnw test`.
- Reproduce bugs by adding integration tests that mock HTTP requests to `/api/@{username}/**`.

---

## Key Files
- `src/main/resources/application.properties`: Main configuration file.
- `src/main/java/vn/autobot/webhook/config/SecurityConfig.java`: Security rules and path permissions.
- `src/main/java/vn/autobot/webhook/controller/ApiMockController.java`: Entry point for all mock webhook traffic.
- `src/main/resources/db/migration/`: Database schema evolution scripts.
