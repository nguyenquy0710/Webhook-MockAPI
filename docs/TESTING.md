# Testing Guide - WebHookMock

## 📋 Mục lục / Table of Contents
- [Testing Overview](#testing-overview)
- [Unit Tests](#unit-tests)
- [Integration Tests](#integration-tests)
- [Running Tests](#running-tests)
- [Writing Tests](#writing-tests)
- [Test Coverage](#test-coverage)
- [Test Data Management](#test-data-management)

---

## Testing Overview

### Test Levels
WebHookMock uses a multi-level testing approach:

| Level | Purpose | Framework | Coverage Target |
|-------|---------|-----------|-----------------|
| **Unit Tests** | Test individual service methods | JUnit 5 + Mockito | > 80% code coverage |
| **Integration Tests** | Test controller endpoints and database interactions | Spring Boot Test | All controllers covered |
| **System Tests** | Test complete user workflows | Spring Boot Test | Critical user flows |
| **Acceptance Tests** | Validate business requirements | Spring Boot Test | PRD requirements |

### Test Tools
- **JUnit 5**: Unit and integration testing
- **Mockito**: Mock dependencies
- **Spring Boot Test**: Integration testing
- **Testcontainers**: Database testing (optional)
- **Selenium/WebDriver**: UI testing (optional)

---

## Unit Tests

### Location
```
src/test/java/vn/autobot/webhook/service/
├── UserServiceTest.java
├── ApiConfigServiceTest.java
├── ApiMockServiceTest.java
├── RequestLogServiceTest.java
└── WebSocketServiceTest.java
```

### UserService Tests

#### testRegisterUser
```java
@Test
void testRegisterUser() {
    // Given
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    
    // When
    User user = userService.registerUser(request);
    
    // Then
    assertNotNull(user.getId());
    assertEquals("testuser", user.getUsername());
    assertTrue(passwordEncoder.matches("password123", user.getPassword()));
    assertEquals("USER", user.getRole());
}
```

#### testRegisterUser_DuplicateUsername
```java
@Test
void testRegisterUser_DuplicateUsername() {
    // Given
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("existinguser");
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    
    // When & Then
    assertThrows(RuntimeException.class, () -> {
        userService.registerUser(request);
    });
}
```

#### testToggleUserRole
```java
@Test
void testToggleUserRole() {
    // Given
    User user = createUser("testuser", "USER");
    
    // When
    User toggledUser = userService.toggleUserRole(user.getId());
    
    // Then
    assertEquals("ADMIN", toggledUser.getRole());
}
```

---

### ApiConfigService Tests

#### testSaveApiConfig
```java
@Test
void testSaveApiConfig() {
    // Given
    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath("/webhook/test");
    dto.setMethod("POST");
    dto.setContentType("application/json");
    dto.setStatusCode(200);
    dto.setResponseBody("{\"message\": \"Hello\"}");
    
    // When
    ApiConfig config = apiConfigService.saveApiConfig("testuser", dto);
    
    // Then
    assertNotNull(config.getId());
    assertEquals("/webhook/test", config.getPath());
    assertEquals("POST", config.getMethod());
}
```

#### testFindApiConfig
```java
@Test
void testFindApiConfig() {
    // Given
    ApiConfig config = createApiConfig("testuser", "/webhook/test", "POST");
    
    // When
    Optional<ApiConfig> found = apiConfigService.findApiConfig("testuser", "/webhook/test", "POST");
    
    // Then
    assertTrue(found.isPresent());
    assertEquals(config.getId(), found.get().getId());
}
```

---

### RequestLogService Tests

#### testLogRequest
```java
@Test
void testLogRequest() {
    // Given
    User user = createUser("testuser");
    HttpServletRequest request = createMockRequest("POST", "/webhook/test");
    
    // When
    RequestLog log = requestLogService.logRequest(user, request, "{}", 200, "{}");
    
    // Then
    assertNotNull(log.getId());
    assertEquals("POST", log.getMethod());
    assertEquals("/webhook/test", log.getPath());
    assertNotNull(log.getCurl());
}
```

#### testExportToExcel
```java
@Test
void testExportToExcel() throws IOException {
    // Given
    User user = createUser("testuser");
    createRequestLogs(user, 10);
    
    HttpServletResponse response = createMockResponse();
    
    // When
    requestLogService.exportToExcel("testuser", response);
    
    // Then
    verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
}
```

---

### WebSocketService Tests

#### testSendRequestUpdate
```java
@Test
void testSendRequestUpdate() {
    // Given
    User user = createUser("testuser");
    
    // When
    webSocketService.sendRequestUpdate("testuser");
    
    // Then
    // Verify message sent to /topic/requests/testuser
    verify(messageTemplate).convertAndSend(eq("/topic/requests/testuser"), any());
}
```

---

## Integration Tests

### Location
```
src/test/java/vn/autobot/webhook/controller/
├── AuthControllerTest.java
├── ApiConfigControllerTest.java
├── RequestLogControllerTest.java
└── AdminControllerTest.java
```

### AuthController Tests

#### testRegisterUser
```java
@Test
void testRegisterUser() throws Exception {
    // Given
    RegisterRequestDto request = new RegisterRequestDto();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    
    // When & Then
    mockMvc.perform(post("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?registered"));
}
```

#### testLoginSuccess
```java
@Test
void testLoginSuccess() throws Exception {
    // Given
    User user = createUser("testuser");
    user.setPassword(passwordEncoder.encode("password123"));
    
    // When & Then
    mockMvc.perform(post("/login")
            .param("username", "testuser")
            .param("password", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"))
        .andExpect(flash().attributeExists("successMessage"));
}
```

---

### ApiConfigController Tests

#### testGetApiConfigs
```java
@Test
void testGetApiConfigs() throws Exception {
    // Given
    User user = createUser("testuser");
    createApiConfig(user, "/webhook/test", "POST");
    
    // When & Then
    mockMvc.perform(get("/api/configs/@{username}", "testuser")
            .with(user(user).password("password123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordsTotal").value(1))
        .andExpect(jsonPath("$.data[0].path").value("/webhook/test"));
}
```

---

### RequestLogController Tests

#### testGetLogs
```java
@Test
void testGetLogs() throws Exception {
    // Given
    User user = createUser("testuser");
    createRequestLogs(user, 10);
    
    // When & Then
    mockMvc.perform(get("/api/logs/@{username}", "testuser")
            .param("page", "0")
            .param("size", "10")
            .with(user(user).password("password123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(10));
}
```

---

## Running Tests

### Run All Tests
```bash
# Linux/macOS
./mvnw test

# Windows
mvnw.cmd test
```

### Run Single Test Class
```bash
# Linux/macOS
./mvnw test -Dtest=UserServiceTest

# Windows
mvnw.cmd test -Dtest=UserServiceTest
```

### Run Single Test Method
```bash
# Linux/macOS
./mvnw test -Dtest=UserServiceTest#testRegisterUser

# Windows
mvnw.cmd test -Dtest=UserServiceTest#testRegisterUser
```

### Run Tests with Coverage
```bash
# Linux/macOS
./mvnw clean test jacoco:report

# Windows
mvnw.cmd clean test jacoco:report
```

### Run Tests in Parallel
```bash
# Linux/macOS
./mvnw test -DforkCount=4

# Windows
mvnw.cmd test -DforkCount=4
```

---

## Writing Tests

### Test Naming Convention
- Use descriptive test names
- Follow pattern: `testMethodName_State_ExpectedBehavior`
- Example: `testRegisterUser_DuplicateUsername_ThrowsException`

### Test Structure
```java
@Test
void testName() {
    // Given - Setup test data
    // When - Execute method under test
    // Then - Verify results
}
```

### Best Practices
- Test one thing per test method
- Use meaningful assertions
- Test edge cases and error conditions
- Use @BeforeEach for common setup
- Use @AfterEach for cleanup

### Example Test Class
```java
@SpringBootTest
@AutoConfigureMockMvc
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        // When
        User user = userService.registerUser(request);

        // Then
        assertNotNull(user.getId());
        assertEquals("testuser", user.getUsername());
        assertTrue(passwordEncoder.matches("password123", user.getPassword()));
        assertEquals("USER", user.getRole());
    }

    @Test
    void testRegisterUser_DuplicateUsername_ThrowsException() {
        // Given
        createUser("existinguser");
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(request);
        });
    }
}
```

---

## Test Coverage

### Coverage Targets
- **Unit Tests**: > 80% code coverage
- **Integration Tests**: All controllers covered
- **Critical Paths**: 100% coverage

### Generate Coverage Report
```bash
# Linux/macOS
./mvnw clean test jacoco:report

# Windows
mvnw.cmd clean test jacoco:report
```

### View Coverage Report
```bash
# Open report in browser
open target/site/jacoco/index.html
```

### Coverage Metrics
- **Line Coverage**: Percentage of lines executed
- **Branch Coverage**: Percentage of branches executed
- **Method Coverage**: Percentage of methods executed
- **Class Coverage**: Percentage of classes executed

---

## Test Data Management

### Test Data Setup
- Use @BeforeEach for common setup
- Create test data factories
- Use in-memory database for tests

### Test Data Cleanup
- Use @AfterEach for cleanup
- Delete test data after each test
- Reset database between test classes

### Example Test Data Setup
```java
@BeforeEach
void setUp() {
    userRepository.deleteAll();
    createUser("admin", "ADMIN");
    createUser("testuser", "USER");
}

@AfterEach
void tearDown() {
    userRepository.deleteAll();
}
```

### Test Data Factories
```java
private User createUser(String username, String role) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@example.com");
    user.setPassword(passwordEncoder.encode("password123"));
    user.setRole(role);
    return userRepository.save(user);
}

private User createUser(String username) {
    return createUser(username, "USER");
}
```

---

## Continuous Integration

### CI/CD Integration
- Tests run automatically on every build
- Tests run on pull requests
- Coverage report generated
- Build fails if tests fail

### GitHub Actions
```yaml
- name: Run tests
  run: ./mvnw test
```

---

## Troubleshooting

### Test Fails with Database Error
```bash
# Clean and rebuild
./mvnw clean package

# Check database configuration
# Verify H2 database file path
```

### Test Timeout
```bash
# Increase timeout
@Test(timeout = 5000)
void testLongRunningOperation() { ... }
```

### Test Runs Slowly
```bash
# Run tests in parallel
./mvnw test -DforkCount=4

# Skip slow tests
./mvnw test -DskipSlowTests=true
```

---

## Next Steps

- [ ] [Architecture](./ARCHITECTURE.md) - Understand system design
- [ ] [Development Guide](./DEVELOPMENT.md) - Development setup
- [ ] [Deployment Guide](./DEPLOYMENT.md) - Deploy to production
