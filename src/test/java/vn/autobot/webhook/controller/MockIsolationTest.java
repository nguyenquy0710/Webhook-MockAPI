package vn.autobot.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.UserService;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Mock responses share the host with the signed-in pages, so they must not act on its origin or cookies
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:mock-isolation;DB_CLOSE_DELAY=-1"})
class MockIsolationTest {

  private static final String CSP = "Content-Security-Policy";

  @Autowired
  private TestRestTemplate rest;
  @Autowired
  private UserService users;
  @Autowired
  private ApiMockService mocks;

  @BeforeEach
  void createUser() {
    if (!users.existsByUsername("mocker")) {
      users.registerUser(new RegisterRequestDto("mocker", "secret123", "secret123", "mocker@example.com"));
    }
  }

  private ResponseEntity<String> call(String path, String contentType, String body, String headers, int delayMs) {
    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath(path);
    dto.setMethod("GET");
    dto.setContentType(contentType);
    dto.setStatusCode(200);
    dto.setResponseBody(body);
    dto.setResponseHeaders(headers);
    dto.setDelayMs(delayMs);
    mocks.saveApiConfig("mocker", dto);
    return rest.getForEntity("/api/@mocker" + path, String.class);
  }

  @Test
  void htmlMockIsSandboxed() {
    ResponseEntity<String> response = call("/html", "text/html", "<p>hi</p>", null, 0);

    assertEquals("<p>hi</p>", response.getBody());
    assertEquals(Collections.singletonList("sandbox"), response.getHeaders().get(CSP));
  }

  @Test
  void delayedMockIsSandboxedToo() {
    assertEquals(Collections.singletonList("sandbox"), call("/slow", "text/html", "<p>hi</p>", null, 50).getHeaders().get(CSP));
  }

  @Test
  void endpointCspDoesNotReplaceTheSandbox() {
    for (int delay : new int[] {0, 50}) {
      HttpHeaders headers = call("/csp" + delay, "text/html", "<p>hi</p>",
          "{\"Content-Security-Policy\":\"sandbox allow-scripts allow-same-origin\"}", delay).getHeaders();
      assertTrue(headers.get(CSP).containsAll(Arrays.asList("sandbox", "sandbox allow-scripts allow-same-origin")));
    }
  }

  @Test
  void cookieHeadersAreDroppedAndOtherHeadersAreKept() {
    HttpHeaders headers = call("/cookie", "application/json", "{\"a\":1}",
        "{\"set-cookie\":\"JSESSIONID=fixed\",\"Clear-Site-Data\":\"\\\"cookies\\\"\",\"X-Custom\":\"yes\"}", 0).getHeaders();

    assertNull(headers.get(HttpHeaders.SET_COOKIE));
    assertNull(headers.get("Clear-Site-Data"));
    assertEquals("yes", headers.getFirst("X-Custom"));
  }

  @Test
  void signedInPagesAreNotSandboxed() {
    assertFalse(rest.getForEntity("/login", String.class).getHeaders().containsKey(CSP));
  }
}
