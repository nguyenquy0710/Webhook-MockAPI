package vn.autobot.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import vn.autobot.webhook.TestBrowser;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Who can open request logs and the H2 console, through the real security filter chain
class AccessControlTest {

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
      "spring.datasource.url=jdbc:h2:mem:access-default;DB_CLOSE_DELAY=-1"})
  class Defaults extends Base {

    @Test
    void ownerReadsOwnLogs() {
      assertEquals(200, get("/view/@owner", login("owner", "secret123")));
    }

    @Test
    void otherUserCannotReadLogs() {
      assertEquals(403, get("/view/@owner", login("other", "secret123")));
    }

    @Test
    void adminReadsOtherUsersLogsOnlyThroughAdminPage() {
      TestBrowser admin = login("admin", "admin123");

      assertEquals(200, get("/admin/user/@owner/logs", admin));
      assertEquals(403, get("/view/@owner", admin));
    }

    @Test
    void h2ConsoleIsOffByDefault() {
      assertEquals(404, get("/h2-console/", login("admin", "admin123")));
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
      "spring.datasource.url=jdbc:h2:mem:access-console;DB_CLOSE_DELAY=-1", "spring.h2.console.enabled=true"})
  class ConsoleEnabled extends Base {

    @Test
    void h2ConsoleNeedsAdmin() {
      assertEquals(302, get("/h2-console/", new TestBrowser(port))); // to the app's login page
      assertEquals(403, get("/h2-console/", login("other", "secret123")));
      assertEquals(200, get("/h2-console/", login("admin", "admin123")));
    }
  }

  abstract static class Base {

    @LocalServerPort
    int port;
    @Autowired
    UserService users;

    @BeforeEach
    void createUsers() {
      for (String name : new String[] {"owner", "other"}) {
        if (!users.existsByUsername(name)) {
          users.registerUser(new RegisterRequestDto(name, "secret123", "secret123", name + "@example.com"));
        }
      }
    }

    TestBrowser login(String username, String password) {
      return new TestBrowser(port).login(username, password);
    }

    int get(String path, TestBrowser browser) {
      return browser.get(path).statusCode();
    }
  }
}
