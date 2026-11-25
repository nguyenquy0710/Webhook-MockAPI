package vn.autobot.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import vn.autobot.webhook.TestBrowser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Requests that change data with the session cookie need a CSRF token; public endpoints do not
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:csrf;DB_CLOSE_DELAY=-1")
class CsrfTest {

  @LocalServerPort
  private int port;
  private TestBrowser admin;

  @BeforeEach
  void signIn() {
    admin = new TestBrowser(port).login("admin", "admin123");
  }

  @Test
  void cookieRequestsWithoutTokenAreRefused() {
    for (String path : new String[] {"/api-configs", "/api-configs/1", "/api-configs/1/delete", "/mcp-token/renew",
        "/change-password", "/view/@admin/delete-all", "/api/configs/backup/@admin", "/api/configs/restore/@admin",
        "/admin/settings/general", "/admin/settings/mcp", "/admin/settings/maintenance/run", "/admin/users/create",
        "/admin/users/1/delete", "/admin/users/1/toggle-role", "/admin/users/1/toggle-status", "/logout"}) {
      assertEquals(403, admin.send("POST", path, "a=b").statusCode(), path);
    }
    assertEquals(403, admin.send("DELETE", "/api/logs/@admin", "").statusCode());
    assertEquals(200, admin.get("/dashboard").statusCode()); // still signed in
    assertEquals(403, new TestBrowser(port).send("POST", "/login", "username=admin&password=admin123").statusCode());
    assertEquals(403, new TestBrowser(port).send("POST", "/register", "username=x").statusCode());
  }

  @Test
  void formsAndFetchWorkWithTheToken() {
    String token = admin.csrfField("/api-configs");
    assertEquals(302, admin.send("POST", "/mcp-token/renew", token.substring(1)).statusCode());
    assertEquals("/api-configs?saved", location("/api-configs",
        "path=/a&method=POST&contentType=application/json&statusCode=200&responseBody={}" + token));

    Matcher meta = Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"").matcher(admin.get("/api-configs").body());
    assertTrue(meta.find());
    assertEquals(200, admin.send("POST", "/api/configs/backup/@admin", "", "X-CSRF-TOKEN", meta.group(1)).statusCode());
    assertEquals(200, admin.send("DELETE", "/api/logs/@admin", "", "X-CSRF-TOKEN", meta.group(1)).statusCode());
  }

  @Test
  void everyPostFormCarriesTheToken() {
    for (String page : new String[] {"/dashboard", "/api-configs", "/change-password", "/view/@admin", "/admin/users",
        "/admin/users/create", "/admin/users/1", "/admin/settings"}) {
      Matcher form = Pattern.compile("<form[^>]*method=\"post\".*?</form>", Pattern.DOTALL).matcher(admin.get(page).body());
      int forms = 0;
      while (form.find()) {
        forms++;
        assertTrue(form.group().contains("name=\"_csrf\""), page + ": " + form.group());
      }
      assertTrue(forms > 0, page);
    }
  }

  @Test
  void mockAndMcpEndpointsNeedNoToken() {
    admin.send("POST", "/api-configs",
        "path=/open&method=POST&contentType=application/json&statusCode=200&responseBody={}" + admin.csrfField("/api-configs"));
    TestBrowser caller = new TestBrowser(port);
    assertEquals(200, caller.send("POST", "/api/@admin/open", "a=b").statusCode());
    assertEquals(401, caller.send("POST", "/mcp/@admin/mcp", "{}").statusCode()); // reached the Bearer check
  }

  @Test
  void logoutNeedsPostAndEndsTheSession() {
    admin.get("/logout");
    assertEquals(200, admin.get("/dashboard").statusCode());

    assertEquals("/login?logout", location("/logout", admin.csrfField("/dashboard").substring(1)));
    assertEquals(302, admin.get("/dashboard").statusCode());
  }

  private String location(String path, String form) {
    String location = admin.send("POST", path, form).headers().firstValue("Location").orElse("");
    return location.replaceFirst("^https?://[^/]+", "");
  }
}
