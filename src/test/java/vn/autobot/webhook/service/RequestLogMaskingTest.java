package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.autobot.webhook.model.RequestLog;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.RequestLogRepository;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Credentials that callers send must not reach the stored log
class RequestLogMaskingTest {

  private static final String SECRET = "S3CR3T";

  private RequestLogService service;

  @BeforeEach
  void setUp() {
    RequestLogRepository repository = mock(RequestLogRepository.class);
    when(repository.save(any(RequestLog.class))).thenAnswer(i -> i.getArgument(0));
    service = new RequestLogService(repository, mock(UserService.class), mock(WebSocketService.class), new ObjectMapper());
  }

  private RequestLog log(MockHttpServletRequest request, String body, String responseBody) {
    return service.logRequest(new User(), request, body, 200, responseBody);
  }

  @Test
  void secretsAreMaskedEverywhereButTheBody() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/@u/p");
    request.addHeader("Authorization", "Bearer " + SECRET);
    request.addHeader("X-Api-Key", SECRET);
    request.addHeader("X-Custom", "kept");
    request.setCookies(new Cookie("sid", SECRET));
    request.setQueryString("access_token=" + SECRET + "&%70assword=" + SECRET + "&page=2&flag");
    request.setParameter("access_token", SECRET);
    request.setParameter("page", "2");

    RequestLog log = log(request, "{\"password\":\"in-body\"}",
        "{\"a\":\"{{headers.Authorization}}\",\"c\":\"{{cookies.sid}}\",\"q\":\"{{queryParams.access_token}}\",\"p\":\"{{params.access_token}}\"}");

    for (String stored : new String[] {log.getRequestHeaders(), log.getQueryParams(), log.getCurl(), log.getResponseBody()}) {
      assertFalse(stored.contains(SECRET), stored);
      assertTrue(stored.contains("***"), stored);
    }
    assertTrue(log.getRequestHeaders().contains("kept"));
    assertTrue(log.getQueryParams().contains("page=2"));
    assertTrue(log.getCurl().contains("?access_token=***&%70assword=***&page=2&flag'"), log.getCurl());
    assertEquals("{\"password\":\"in-body\"}", log.getRequestBody());
  }

  @Test
  void malformedQueryIsStillLogged() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/@u/p");
    request.setQueryString("%zz=1&page=2");

    assertTrue(log(request, null, null).getCurl().contains("?%zz=1&page=2'"));
  }

  @Test
  void curlQuotesCannotBeBroken() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/@u/p");
    request.addHeader("X-Q", "a'; touch x; '");
    request.setQueryString("q='$(id)'");

    String curl = log(request, "it's", null).getCurl();

    assertTrue(curl.contains("-H 'X-Q: a'\\''; touch x; '\\'''"), curl);
    assertTrue(curl.contains("-d 'it'\\''s'"), curl);
    assertTrue(curl.endsWith("?q='\\''$(id)'\\'''"), curl);
  }
}
