package vn.autobot.webhook.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import vn.autobot.webhook.TestBrowser;

import java.net.http.HttpHeaders;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Browser headers of the pages, and integrity of the scripts and styles they load from CDNs
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:headers;DB_CLOSE_DELAY=-1")
class PageHeadersTest {

  private static final String[] NAMES = {"Referrer-Policy", "Permissions-Policy", "Content-Security-Policy-Report-Only"};

  @LocalServerPort
  private int port;

  @Test
  void pagesHaveTheHeadersAndMockResponsesDoNot() {
    HttpHeaders page = new TestBrowser(port).get("/login").headers();
    HttpHeaders mock = new TestBrowser(port).get("/api/@admin/none").headers();

    for (String name : NAMES) {
      assertTrue(page.firstValue(name).isPresent(), name);
      assertFalse(mock.firstValue(name).isPresent(), name);
    }
  }

  @Test
  void everyCdnFileIsPinnedWithIntegrity() {
    TestBrowser admin = new TestBrowser(port).login("admin", "admin123");

    for (String path : new String[] {"/dashboard", "/swagger-ui"}) {
      Matcher tag = Pattern.compile("<(?:script|link)\\b[^>]*(?:src|href)=\"https://[^>]*>").matcher(admin.get(path).body());
      int found = 0;
      while (tag.find()) {
        found++;
        assertTrue(tag.group().contains("integrity=\"sha384-") && tag.group().contains("crossorigin"), tag.group());
        assertTrue(tag.group().matches("(?s).*[@/-]\\d+\\.\\d+\\.\\d+.*"), tag.group());
      }
      assertTrue(found > 0, path);
    }
  }

  @Test
  void logoIsServedLocallyWithoutLogin() {
    assertEquals(200, new TestBrowser(port).get("/images/logo-webhook.png").statusCode());
  }
}
