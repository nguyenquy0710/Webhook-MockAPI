package vn.autobot.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import vn.autobot.webhook.TestBrowser;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.RegisterRequestDto;
import vn.autobot.webhook.service.ApiMockService;
import vn.autobot.webhook.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Bad input and missing records answer with 4xx or a sensible page, never with a server error
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:errors;DB_CLOSE_DELAY=-1")
class ErrorHandlingTest {

  @LocalServerPort
  private int port;
  @Autowired
  private UserService users;
  @Autowired
  private ApiMockService mocks;
  private TestBrowser admin;

  @BeforeEach
  void signIn() {
    admin = new TestBrowser(port).login("admin", "admin123");
  }

  @Test
  void anonymousJsonCallsGet401AndPagesGoToLogin() {
    TestBrowser anonymous = new TestBrowser(port);

    for (String path : new String[] {"/api/logs/@admin", "/api/logs/@nobody", "/api/logs/@admin/count",
        "/api/configs/@admin?draw=1&start=0&length=10"}) {
      assertEquals(401, anonymous.get(path).statusCode(), path);
      assertEquals(401, anonymous.get(path, "Accept", "text/html").statusCode(), path);
    }
    assertEquals(302, anonymous.get("/dashboard").statusCode());
    assertEquals(302, anonymous.get("/dashboard", "Accept", "text/html").statusCode());
    assertEquals(404, anonymous.get("/api/@admin/none").statusCode()); // mock endpoints stay public
  }

  @Test
  void onlyListedColumnsCanBeSorted() {
    for (String sort : new String[] {"user.password", "user.id", "nope", "timestamp,desc&sort=user.email"}) {
      assertEquals(400, admin.get("/api/logs/@admin?sort=" + sort).statusCode(), sort);
    }
    assertEquals(200, admin.get("/api/logs/@admin?sort=timestamp,desc&sort=id").statusCode());
    assertEquals(200, admin.get("/api/logs/@admin").statusCode());
  }

  @Test
  void pagingIsBroughtIntoRange() {
    for (String page : new String[] {"size=0", "size=-5", "size=100000", "page=-1", "page=2147483647&size=2147483647"}) {
      assertEquals(200, admin.get("/view/@admin?" + page).statusCode(), page);
      assertEquals(200, admin.get("/admin/user/@admin/logs?" + page).statusCode(), page);
    }
    for (String range : new String[] {"start=0&length=0", "start=0&length=-1", "start=-10&length=10", "start=2147483647&length=1"}) {
      assertEquals(200, admin.get("/api/configs/@admin?draw=1&" + range).statusCode(), range);
    }
  }

  @Test
  void missingAndForeignRecordsLookTheSame() {
    if (!users.existsByUsername("errowner")) {
      users.registerUser(new RegisterRequestDto("errowner", "secret123", "secret123", "errowner@example.com"));
    }
    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath("/mine" + System.nanoTime());
    dto.setMethod("GET");
    dto.setContentType("application/json");
    dto.setStatusCode(200);
    long foreign = mocks.saveApiConfig("errowner", dto).getId();

    assertEquals(404, admin.get("/api-configs/" + foreign).statusCode());
    assertEquals(404, admin.get("/api-configs/999999").statusCode());
    assertEquals(404, admin.get("/admin/users/999999").statusCode());
    assertEquals(404, admin.get("/admin/user/@nobody/logs").statusCode());
    assertEquals(200, new TestBrowser(port).login("errowner", "secret123").get("/api-configs/" + foreign).statusCode());
  }
}
