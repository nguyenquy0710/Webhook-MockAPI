package vn.autobot.webhook.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.util.ReflectionTestUtils;
import vn.autobot.webhook.TestBrowser;
import vn.autobot.webhook.service.McpRateLimiter;

import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Attempts per minute on /login and /register; each test uses its own client address and a stopped clock
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:authlimit;DB_CLOSE_DELAY=-1")
class AuthRateLimitTest {

  @LocalServerPort
  private int port;
  @Autowired
  private McpRateLimiter limiter;
  private Instant now = Instant.parse("2026-01-01T00:00:10Z");

  @BeforeEach
  void stopClock() {
    setClock();
  }

  private void setClock() {
    ReflectionTestUtils.setField(limiter, "clock", Clock.fixed(now, ZoneOffset.UTC));
  }

  private HttpResponse<String> post(TestBrowser browser, String path, String form, String address) {
    return browser.send("POST", path, form + browser.csrfField(path), "X-Forwarded-For", address, "Accept", "text/html");
  }

  private HttpResponse<String> login(String username, String password, String address) {
    return post(new TestBrowser(port), "/login", "username=" + username + "&password=" + password, address);
  }

  @Test
  void eleventhAttemptOnOneUsernameIsRefusedUntilTheNextMinute() {
    for (int i = 0; i < 10; i++) {
      assertEquals(302, login("admin", "bad", "198.51.100.1").statusCode());
    }
    HttpResponse<String> refused = login("Admin", "admin123", "198.51.100.1"); // even with the right password
    assertEquals(429, refused.statusCode());
    assertEquals("50", refused.headers().firstValue("Retry-After").orElse(null));
    assertTrue(refused.body().contains("Too many attempts"));

    assertEquals(302, login("someone", "bad", "198.51.100.1").statusCode()); // other username
    assertEquals(302, login("admin", "bad", "198.51.100.2").statusCode()); // other address: nobody is locked out

    now = now.plus(Duration.ofMinutes(1));
    setClock();
    assertTrue(login("admin", "admin123", "198.51.100.1").headers().firstValue("Location").orElse("").endsWith("/dashboard"));
  }

  @Test
  void manyUsernamesFromOneAddressAreLimited() {
    for (int i = 0; i < 30; i++) {
      assertEquals(302, login("user" + i, "bad", "198.51.100.3").statusCode());
    }
    assertEquals(429, login("another", "bad", "198.51.100.3").statusCode());
  }

  @Test
  void registrationIsLimited() {
    for (int i = 0; i < 5; i++) {
      assertEquals(200, post(new TestBrowser(port), "/register", "username=x", "198.51.100.4").statusCode()); // invalid form
    }
    assertEquals(429, post(new TestBrowser(port), "/register", "username=x", "198.51.100.4").statusCode());
  }

  @Test
  void pagesAndOtherRequestsAreNotLimited() {
    for (int i = 0; i < 40; i++) {
      assertEquals(200, new TestBrowser(port).get("/login", "X-Forwarded-For", "198.51.100.5").statusCode());
      assertEquals(404, new TestBrowser(port).send("POST", "/api/@admin/none", "a=b", "X-Forwarded-For", "198.51.100.5").statusCode());
    }
  }
}
