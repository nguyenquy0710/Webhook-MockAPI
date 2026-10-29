package vn.autobot.webhook.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import vn.autobot.webhook.model.McpCall;
import vn.autobot.webhook.model.RequestLog;
import vn.autobot.webhook.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The JPQL behind MCP statistics, log paging and the activity page, run on a real (in-memory) H2
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class McpQueriesTest {

  @Autowired
  private UserRepository users;
  @Autowired
  private RequestLogRepository logs;
  @Autowired
  private McpCallRepository calls;

  private User demo;
  private User other;
  private final LocalDateTime day1 = LocalDate.of(2026, 10, 27).atTime(10, 0);
  private final LocalDateTime day2 = LocalDate.of(2026, 10, 28).atTime(12, 0);

  private User user(String name) {
    User u = new User();
    u.setUsername(name);
    u.setPassword("x");
    u.setEmail(name + "@example.test");
    return users.save(u);
  }

  private void log(User u, String method, String path, int status, LocalDateTime at) {
    RequestLog l = new RequestLog();
    l.setUser(u);
    l.setMethod(method);
    l.setPath(path);
    l.setResponseStatus(status);
    RequestLog saved = logs.saveAndFlush(l); // @PrePersist stamps "now", so set the wanted time with an update
    saved.setTimestamp(at);
    logs.saveAndFlush(saved);
  }

  private void call(User u, String tool, boolean ok, String args, String client, LocalDateTime at) {
    McpCall c = new McpCall();
    c.setUser(u);
    c.setTool(tool);
    c.setResultOk(ok);
    c.setArguments(args);
    c.setClientName(client);
    c.setCreatedAt(at);
    calls.save(c);
  }

  @BeforeEach
  void setUp() {
    demo = user("demo");
    other = user("other");
    log(demo, "GET", "/api/@demo/orders", 200, day1);
    log(demo, "GET", "/api/@demo/orders", 200, day1.plusHours(1));
    log(demo, "POST", "/api/@demo/orders", 201, day1.plusHours(2));
    log(demo, "GET", "/api/@demo/orders/1", 404, day2);
    log(demo, "POST", "/api/@demo/pay", 503, day2.plusHours(1));
    log(other, "GET", "/api/@other/x", 200, day1); // never visible to demo
  }

  @Test
  void countsAndGroupsOverARangeForOneUser() {
    LocalDateTime from = LocalDate.of(2026, 10, 27).atStartOfDay();
    LocalDateTime to = LocalDate.of(2026, 10, 29).atStartOfDay();

    assertEquals(5, logs.countInRange(demo, from, to, ""));
    assertEquals(3, logs.countInRange(demo, from, to, "/api/@demo/orders"));
    assertEquals(1, logs.countInRange(demo, from, to, "/api/@demo/orders/1"));

    List<Object[]> byMethod = logs.countByMethodInRange(demo, from, to, "");
    assertEquals("GET", byMethod.get(0)[0]);
    assertEquals(3L, ((Number) byMethod.get(0)[1]).longValue());
    assertEquals(2L, ((Number) byMethod.get(1)[1]).longValue());

    List<Object[]> byClass = logs.countByStatusClassInRange(demo, from, to, "");
    assertEquals(3, byClass.size());
    assertEquals(2, ((Number) byClass.get(0)[0]).intValue());
    assertEquals(3L, ((Number) byClass.get(0)[1]).longValue()); // 200, 200, 201
    assertEquals(4, ((Number) byClass.get(1)[0]).intValue());
    assertEquals(5, ((Number) byClass.get(2)[0]).intValue());

    List<Object[]> byDay = logs.countByDayInRange(demo, from, to, "");
    assertEquals(2, byDay.size());
    assertEquals(2026, ((Number) byDay.get(0)[0]).intValue());
    assertEquals(10, ((Number) byDay.get(0)[1]).intValue());
    assertEquals(27, ((Number) byDay.get(0)[2]).intValue());
    assertEquals(3L, ((Number) byDay.get(0)[3]).longValue());
    assertEquals(28, ((Number) byDay.get(1)[2]).intValue());
    assertEquals(2L, ((Number) byDay.get(1)[3]).longValue());

    List<Object[]> top = logs.topPathsInRange(demo, from, to, "", PageRequest.of(0, 2));
    assertEquals(2, top.size());
    assertEquals("/api/@demo/orders", top.get(0)[0]);
    assertEquals(3L, ((Number) top.get(0)[1]).longValue());
  }

  @Test
  void rangeEndIsExclusive() {
    LocalDateTime from = LocalDate.of(2026, 10, 27).atStartOfDay();

    assertEquals(3, logs.countInRange(demo, from, LocalDate.of(2026, 10, 28).atStartOfDay(), ""));
    assertEquals(0, logs.countInRange(demo, from.minusDays(5), from, ""));
  }

  @Test
  void searchByPathIsExactAndFiltersMethodAndStatus() {
    PageRequest first = PageRequest.of(0, 10);

    assertEquals(3, logs.searchByPath(demo, "/api/@demo/orders", "", 0, Integer.MAX_VALUE, first).getTotalElements()); // not /orders/1
    assertEquals(2, logs.searchByPath(demo, "/api/@demo/orders", "GET", 0, Integer.MAX_VALUE, first).getTotalElements());
    assertEquals(1, logs.searchByPath(demo, "/api/@demo/orders", "", 200, 300, first).getContent().stream()
        .filter(l -> l.getResponseStatus() == 201).count());
    assertEquals(0, logs.searchByPath(demo, "/api/@other/x", "", 0, Integer.MAX_VALUE, first).getTotalElements());

    List<RequestLog> page = logs.searchByPath(demo, "/api/@demo/orders", "", 0, Integer.MAX_VALUE, PageRequest.of(0, 2)).getContent();
    assertEquals(2, page.size());
    assertTrue(page.get(0).getTimestamp().isAfter(page.get(1).getTimestamp())); // newest first
  }

  @Test
  void activitySearchFiltersByToolResultAndText() {
    LocalDateTime now = LocalDateTime.of(2026, 10, 29, 10, 0);
    call(demo, "list_api_configs", true, "{\"page\":0}", "claude-code 2.1", now);
    call(demo, "update_api_config", true, "{\"id\":12}", "cursor", now.plusMinutes(1));
    call(demo, "update_api_config", false, "{\"id\":99}", "cursor", now.plusMinutes(2));
    call(other, "list_api_configs", true, "{}", "curl", now);
    PageRequest p = PageRequest.of(0, 10);

    assertEquals(3, calls.search(demo, "", 0, "", p).getTotalElements());
    assertEquals("update_api_config", calls.search(demo, "", 0, "", p).getContent().get(0).getTool()); // newest first
    assertEquals(2, calls.search(demo, "update_api_config", 0, "", p).getTotalElements());
    assertEquals(1, calls.search(demo, "update_api_config", 2, "", p).getTotalElements());
    assertEquals(2, calls.search(demo, "", 1, "", p).getTotalElements());
    assertEquals(1, calls.search(demo, "", 0, "CLAUDE", p).getTotalElements()); // client, case-insensitive
    assertEquals(1, calls.search(demo, "", 0, "\"id\":99", p).getTotalElements()); // arguments
  }

  @Test
  void activityCountsAreScopedByUserAndTime() {
    LocalDateTime since = LocalDateTime.of(2026, 10, 29, 0, 0);
    call(demo, "update_api_config", true, "{}", "a", since.plusHours(1));
    call(demo, "update_api_config", false, "{}", "a", since.plusHours(2));
    call(demo, "list_api_configs", true, "{}", "a", since.plusHours(3));
    call(demo, "create_api_config", true, "{}", "a", since.minusDays(1)); // yesterday
    call(other, "update_api_config", true, "{}", "a", since.plusHours(1));

    assertEquals(3, calls.countByUserAndCreatedAtGreaterThanEqual(demo, since));
    assertEquals(1, calls.countByUserAndResultOkFalseAndCreatedAtGreaterThanEqual(demo, since));
    assertEquals(1, calls.countByUserAndResultOkTrueAndToolInAndCreatedAtGreaterThanEqual(demo,
        Arrays.asList("create_api_config", "update_api_config"), since));
    assertEquals(4, calls.countByCreatedAtGreaterThanEqual(since));
    assertEquals(1, calls.deleteOlderThan(since)); // the one from yesterday
  }
}
