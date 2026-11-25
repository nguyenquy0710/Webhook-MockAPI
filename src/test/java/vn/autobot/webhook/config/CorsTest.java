package vn.autobot.webhook.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import vn.autobot.webhook.TestBrowser;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// What pages on other origins may read
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:cors;DB_CLOSE_DELAY=-1", "app.domain=https://public.example/base"})
class CorsTest {

  private static final String EVIL = "https://evil.example";
  private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
  private static final String ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  @LocalServerPort
  private int port;

  @Test
  void cookiePagesGiveOtherOriginsNothing() {
    TestBrowser admin = new TestBrowser(port).login("admin", "admin123");

    for (String path : new String[] {"/dashboard", "/api/logs/@admin", "/api/configs/@admin?draw=1&start=0&length=10"}) {
      HttpResponse<String> response = admin.get(path, "Origin", EVIL);
      assertEquals(200, response.statusCode(), path);
      assertFalse(response.headers().firstValue(ALLOW_ORIGIN).isPresent(), path);
      assertFalse(response.headers().firstValue(ALLOW_CREDENTIALS).isPresent(), path);
    }
  }

  @Test
  void preflightToCookiePagesIsRefused() {
    HttpResponse<String> response = new TestBrowser(port).send("OPTIONS", "/api/logs/@admin", "",
        "Origin", EVIL, "Access-Control-Request-Method", "DELETE");

    assertEquals(401, response.statusCode());
    assertFalse(response.headers().firstValue(ALLOW_ORIGIN).isPresent());
  }

  @Test
  void publicEndpointsAllowAnyOriginWithoutCredentials() {
    for (String path : new String[] {"/api/@admin/orders", "/mcp/@admin/mcp"}) {
      for (String method : new String[] {"POST", "PATCH"}) {
        HttpResponse<String> response = new TestBrowser(port).send("OPTIONS", path, "",
            "Origin", EVIL, "Access-Control-Request-Method", method, "Access-Control-Request-Headers", "x-custom");
        assertEquals(200, response.statusCode(), path);
        assertEquals("*", response.headers().firstValue(ALLOW_ORIGIN).orElse(null), path);
        assertFalse(response.headers().firstValue(ALLOW_CREDENTIALS).isPresent(), path);
      }
    }
  }

  @Test
  void webSocketAcceptsOnlyThisSite() {
    TestBrowser admin = new TestBrowser(port).login("admin", "admin123");

    assertEquals(403, admin.get("/ws/info", "Origin", EVIL).statusCode());
    assertEquals(200, admin.get("/ws/info", "Origin", "https://public.example").statusCode());
    assertEquals(200, admin.get("/ws/info", "Origin", "http://localhost:" + port).statusCode());
  }
}
