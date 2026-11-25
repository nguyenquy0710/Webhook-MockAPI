package vn.autobot.webhook.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import vn.autobot.webhook.TestBrowser;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.RequestLogService;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The test client connects from loopback, like a reverse proxy on the same machine
class ForwardedHeadersTest {

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
      "spring.datasource.url=jdbc:h2:mem:fwd;DB_CLOSE_DELAY=-1")
  class AppendedHeader extends Base {

    @Test
    void clientCannotChooseItsAddress() throws Exception {
      // The proxy appends the real address as the last value, on the same line or on one of its own
      assertEquals("198.51.100.7", loggedIp(0, "X-Forwarded-For", "203.0.113.66, 198.51.100.7"));
      assertEquals("198.51.100.7", loggedIp(0, "X-Forwarded-For", "127.0.0.1", "X-Forwarded-For", "198.51.100.7"));
      assertEquals("198.51.100.7", loggedIp(50, "X-Forwarded-For", "203.0.113.66, 198.51.100.7"));
    }

    @Test
    void directCallIsLoggedWithItsAddress() throws Exception {
      assertEquals("127.0.0.1", loggedIp(0));
    }

    @Test
    void httpsRequestGetsHttpsRedirectSecureCookieAndHsts() {
      HttpResponse<String> response = new TestBrowser(port).get("/dashboard", "X-Forwarded-Proto", "https",
          "X-Forwarded-Host", "evil.example", "X-Forwarded-Port", "1234");

      assertEquals("https://localhost/login", response.headers().firstValue("Location").orElse("").replace(":" + port, ""));
      String cookie = response.headers().firstValue("Set-Cookie").orElse("");
      assertTrue(cookie.contains("Secure") && cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"), cookie);
      assertTrue(response.headers().firstValue("Strict-Transport-Security").isPresent());
    }

    @Test
    void plainHttpStillWorksWithoutSecureCookie() {
      HttpResponse<String> response = new TestBrowser(port).get("/dashboard");

      assertFalse(response.headers().firstValue("Set-Cookie").orElse("").contains("Secure"));
      assertEquals(200, new TestBrowser(port).login("admin", "admin123").get("/dashboard").statusCode());
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
      "spring.datasource.url=jdbc:h2:mem:fwd2;DB_CLOSE_DELAY=-1", "server.tomcat.remoteip.remote-ip-header=X-Edge-Ip"})
  class OverwrittenHeader extends Base {

    @Test
    void onlyTheConfiguredHeaderCounts() throws Exception {
      assertEquals("198.51.100.7", loggedIp(0, "X-Forwarded-For", "203.0.113.66", "X-Edge-Ip", "198.51.100.7"));
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
      "spring.datasource.url=jdbc:h2:mem:fwd3;DB_CLOSE_DELAY=-1", "server.tomcat.remoteip.internal-proxies=10\\.0\\.0\\.1"})
  class UntrustedProxy extends Base {

    @Test
    void headersFromOtherAddressesAreIgnored() throws Exception {
      assertEquals("127.0.0.1", loggedIp(0, "X-Forwarded-For", "203.0.113.66"));
    }
  }

  abstract static class Base {

    @LocalServerPort
    int port;
    @Autowired
    ApiMockService mocks;
    @Autowired
    RequestLogService logs;

    @BeforeEach
    void clearLogs() {
      logs.deleteAllRequestLogs("admin");
    }

    String loggedIp(int delayMs, String... headers) throws InterruptedException {
      String path = "/ip-" + System.nanoTime();
      ApiConfigDto dto = new ApiConfigDto();
      dto.setPath(path);
      dto.setMethod("GET");
      dto.setContentType("application/json");
      dto.setStatusCode(200);
      dto.setResponseBody("{\"ip\":\"{{remoteAddr}}\"}");
      dto.setDelayMs(delayMs);
      mocks.saveApiConfig("admin", dto);
      String body = new TestBrowser(port).get("/api/@admin" + path, headers).body();
      // The log is written after the response is complete
      for (int i = 0; i < 50 && logs.countRequestLogs("admin", path) == 0; i++) {
        Thread.sleep(100);
      }
      String ip = logs.getRequestLogs("admin", path, PageRequest.of(0, 1)).getContent().get(0).getSourceIp();
      assertEquals("{\"ip\":\"" + ip + "\"}", body);
      return ip;
    }
  }
}
