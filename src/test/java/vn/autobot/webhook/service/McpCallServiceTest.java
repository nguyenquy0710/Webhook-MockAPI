package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import vn.autobot.webhook.model.McpCall;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.McpCallRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// What the activity log stores: redacted, truncated; and how the activity page searches it
class McpCallServiceTest {

  private McpCallRepository repository;
  private McpCallService service;
  private User user;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    repository = mock(McpCallRepository.class);
    UserService users = mock(UserService.class);
    user = new User();
    user.setId(1L);
    user.setUsername("demo");
    when(users.findByUsername("demo")).thenReturn(user);
    service = new McpCallService(repository, users);
  }

  @Test
  void recordStoresRedactedTruncatedValues() throws Exception {
    String bigBody = "x".repeat(5000);

    service.record(user, "create_api_config", mapper.readTree("{\"path\":\"/a\",\"responseHeaders\":\"{}\",\"apiKey\":\"secret\",\"responseBody\":\"" + bigBody + "\"}"),
        true, "y".repeat(5000), 42, new McpService.Caller("c".repeat(300), "k3Fz"));

    ArgumentCaptor<McpCall> saved = ArgumentCaptor.forClass(McpCall.class);
    verify(repository).save(saved.capture());
    McpCall c = saved.getValue();
    assertEquals("create_api_config", c.getTool());
    assertFalse(c.getArguments().contains("secret"));
    assertTrue(c.getArguments().length() <= 2000);
    assertEquals(2000, c.getResponseExcerpt().length());
    assertEquals(100, c.getClientName().length());
    assertEquals("k3Fz", c.getTokenSuffix());
    assertEquals(42, c.getDurationMs());
    assertTrue(c.isResultOk());
    assertNotNull(c.getCreatedAt());
  }

  @Test
  void recordCopesWithMissingArgumentsAndNoCaller() throws Exception {
    service.record(user, "list_api_configs", mapper.missingNode(), false, null, 1, McpService.Caller.NONE);

    ArgumentCaptor<McpCall> saved = ArgumentCaptor.forClass(McpCall.class);
    verify(repository).save(saved.capture());
    assertEquals("{}", saved.getValue().getArguments());
    assertFalse(saved.getValue().isResultOk());
  }

  @Test
  void searchTranslatesTheFilters() {
    service.search("demo", " update_api_config ", "error", " cursor ", PageRequest.of(0, 10));
    verify(repository).search(eq(user), eq("update_api_config"), eq(2), eq("cursor"), any());

    service.search("demo", null, "ok", null, PageRequest.of(0, 10));
    verify(repository).search(eq(user), eq(""), eq(1), eq(""), any());

    service.search("demo", "", "whatever", "", PageRequest.of(0, 10));
    verify(repository).search(eq(user), eq(""), eq(0), eq(""), any());
  }

  @Test
  void statsTodayAndPurgeUseTheRepository() {
    when(repository.countByUserAndCreatedAtGreaterThanEqual(eq(user), any())).thenReturn(5L);
    when(repository.countByUserAndResultOkFalseAndCreatedAtGreaterThanEqual(eq(user), any())).thenReturn(2L);
    when(repository.countByUserAndResultOkTrueAndToolInAndCreatedAtGreaterThanEqual(eq(user), any(), any())).thenReturn(1L);
    when(repository.deleteOlderThan(any())).thenReturn(4);

    McpCallService.Stats s = service.statsToday("demo");

    assertEquals(5, s.getCallsToday());
    assertEquals(2, s.getErrorsToday());
    assertEquals(1, s.getChangesToday());
    assertEquals(4, service.purgeOld());
  }
}
