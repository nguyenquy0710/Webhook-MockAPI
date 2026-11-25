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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// A user's endpoint list is private, and public answers do not reveal which usernames exist
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties =
    "spring.datasource.url=jdbc:h2:mem:swagger;DB_CLOSE_DELAY=-1")
class SwaggerAccessTest {

  @LocalServerPort
  private int port;
  @Autowired
  private UserService users;
  @Autowired
  private ApiMockService mocks;

  @BeforeEach
  void createUser() {
    if (!users.existsByUsername("speco")) {
      users.registerUser(new RegisterRequestDto("speco", "secret123", "secret123", "speco@example.com"));
    }
  }

  @Test
  void anonymousIsSentToLoginForAnyUsername() {
    for (String path : new String[] {"", "/swagger.json", "/generate"}) {
      for (String name : new String[] {"speco", "nobody"}) {
        assertEquals(302, new TestBrowser(port).get("/swagger/@" + name + path).statusCode(), name + path);
      }
    }
  }

  @Test
  void otherUsersAreRefusedForAnyUsername() {
    TestBrowser admin = new TestBrowser(port).login("admin", "admin123");

    for (String path : new String[] {"", "/swagger.json"}) {
      assertEquals(403, admin.get("/swagger/@speco" + path).statusCode());
      assertEquals(403, admin.get("/swagger/@nobody" + path).statusCode());
    }
  }

  @Test
  void ownerSeesTheCurrentEndpoints() {
    TestBrowser owner = new TestBrowser(port).login("speco", "secret123");
    assertEquals(200, owner.get("/swagger/@speco").statusCode());
    assertFalse(owner.get("/swagger/@speco/swagger.json").body().contains("/added-later"));

    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath("/added-later");
    dto.setMethod("GET");
    dto.setContentType("application/json");
    dto.setStatusCode(200);
    mocks.saveApiConfig("speco", dto);

    assertTrue(owner.get("/swagger/@speco/swagger.json").body().contains("/added-later"));
  }

  @Test
  void generalDocumentationStaysPublic() {
    assertEquals(200, new TestBrowser(port).get("/swagger.json").statusCode());
    assertEquals(200, new TestBrowser(port).get("/swagger-ui").statusCode());
  }

  @Test
  void unknownUserAndUnknownPathAnswerAlike() {
    TestBrowser caller = new TestBrowser(port);

    assertEquals(404, caller.get("/api/@nobody/x").statusCode());
    assertEquals(caller.get("/api/@speco/x").body(), caller.get("/api/@nobody/x").body());
  }
}
