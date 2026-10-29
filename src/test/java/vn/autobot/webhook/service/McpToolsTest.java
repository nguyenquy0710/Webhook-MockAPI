package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.dto.RequestLogDto;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The MCP tools: reading and changing endpoints, request log paging and statistics, and the activity record
class McpToolsTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private ApiMockService apis;
  private RequestLogService logs;
  private McpCallService calls;
  private McpService service;
  private User user;
  private ApiConfigDto stored;

  @BeforeEach
  void setUp() {
    apis = mock(ApiMockService.class);
    logs = mock(RequestLogService.class);
    calls = mock(McpCallService.class);
    SettingsService settings = mock(SettingsService.class);
    when(settings.getDomain()).thenReturn("https://mock.test");
    when(settings.getMcpMaxPageSize()).thenReturn(50);
    service = new McpService(apis, logs, calls, settings, javax.validation.Validation.buildDefaultValidatorFactory().getValidator(), mapper);
    user = new User();
    user.setId(1L);
    user.setUsername("demo");

    stored = new ApiConfigDto();
    stored.setId(7L);
    stored.setMethod("GET");
    stored.setPath("/orders");
    stored.setStatusCode(200);
    stored.setContentType("application/json");
    stored.setResponseBody("{\"ok\":true}");
    stored.setDelayMs(0);
    stored.setFailureEnabled(false);
    stored.setFailureCount(3);
    stored.setFailureStatusCodes("503");
    stored.setRetryAfterSeconds(1);
    stored.setUpdatedAt("2026-10-29T10:00:00");
    when(apis.getApiConfig("demo", 7L)).thenReturn(stored);
    when(apis.findApiConfig(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    ApiConfig saved = new ApiConfig();
    saved.setId(8L);
    when(apis.saveApiConfig(eq("demo"), any(ApiConfigDto.class))).thenReturn(saved);
  }

  private JsonNode call(String tool, String args) throws Exception {
    String msg = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"" + tool + "\",\"arguments\":" + args + "}}";
    return service.handle(mapper.readTree(msg), user, new McpService.Caller("claude-code", "k3Fz")).get();
  }

  private JsonNode data(JsonNode response) throws Exception {
    return mapper.readTree(response.at("/result/content/0/text").asText());
  }

  private boolean isError(JsonNode response) {
    return response.at("/result/isError").asBoolean();
  }

  private String errorText(JsonNode response) {
    return response.at("/result/content/0/text").asText();
  }

  @Test
  void toolsListHasAllSixTools() throws Exception {
    JsonNode r = service.handle(mapper.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"), user).get();
    List<String> names = new ArrayList<>();
    r.at("/result/tools").forEach(t -> names.add(t.get("name").asText()));

    assertEquals(List.of("list_api_configs", "get_api_config", "create_api_config", "update_api_config",
        "get_request_log_stats", "list_request_logs"), names);
    assertEquals("method", r.at("/result/tools/2/inputSchema/required/0").asText());
  }

  @Test
  void getApiConfigReturnsEverythingIncludingUpdatedAt() throws Exception {
    JsonNode d = data(call("get_api_config", "{\"id\":7}"));

    assertEquals("/orders", d.get("path").asText());
    assertEquals("{\"ok\":true}", d.get("responseBody").asText());
    assertEquals("2026-10-29T10:00:00", d.get("updatedAt").asText());
    assertFalse(d.get("failureEnabled").asBoolean());
  }

  @Test
  void getApiConfigOfAnotherUserOrMissingIsAToolError() throws Exception {
    when(apis.getApiConfig("demo", 99L)).thenThrow(new RuntimeException("not found"));

    JsonNode r = call("get_api_config", "{\"id\":99}");

    assertTrue(isError(r));
    assertTrue(errorText(r).contains("99"));
    assertTrue(isError(call("get_api_config", "{}")));
  }

  @Test
  void createSavesWithDefaultsAndReturnsTheNewEndpoint() throws Exception {
    when(apis.getApiConfig("demo", 8L)).thenReturn(stored);
    ArgumentCaptor<ApiConfigDto> saved = ArgumentCaptor.forClass(ApiConfigDto.class);

    JsonNode r = call("create_api_config", "{\"method\":\"post\",\"path\":\"/health\",\"statusCode\":201,\"description\":\"Probe\"}");

    assertFalse(isError(r));
    verify(apis).saveApiConfig(eq("demo"), saved.capture());
    assertEquals("POST", saved.getValue().getMethod()); // upper-cased
    assertEquals("application/json", saved.getValue().getContentType());
    assertEquals(0, saved.getValue().getDelayMs());
    assertEquals("Probe", saved.getValue().getDescription());
  }

  @Test
  void createReportsEveryProblemAndDoesNotSave() throws Exception {
    JsonNode r = call("create_api_config", "{\"method\":\"FETCH\",\"path\":\"health\",\"statusCode\":700,\"responseHeaders\":\"[1]\"}");

    assertTrue(isError(r));
    String text = errorText(r);
    assertTrue(text.contains("method must be one of"));
    assertTrue(text.contains("path must start with /"));
    assertTrue(text.contains("Status code must be at most 599"));
    assertTrue(text.contains("responseHeaders must be a JSON object"));
    verify(apis, never()).saveApiConfig(any(), any());
  }

  @Test
  void createRejectsADuplicateMethodAndPath() throws Exception {
    ApiConfig existing = new ApiConfig();
    existing.setId(3L);
    when(apis.findApiConfig("demo", "/orders", "GET")).thenReturn(Optional.of(existing));

    JsonNode r = call("create_api_config", "{\"method\":\"GET\",\"path\":\"/orders\",\"statusCode\":200}");

    assertTrue(isError(r));
    assertTrue(errorText(r).contains("already exists (id 3)"));
  }

  @Test
  void unknownFieldsAndWrongTypesAreExplained() throws Exception {
    assertTrue(errorText(call("create_api_config", "{\"method\":\"GET\",\"path\":\"/a\",\"statusCode\":200,\"colour\":\"red\"}")).contains("Unknown field: colour"));
    assertTrue(errorText(call("create_api_config", "{\"method\":\"GET\",\"path\":\"/a\",\"statusCode\":\"200\"}")).contains("statusCode must be an integer"));
    assertTrue(errorText(call("create_api_config", "{\"method\":\"GET\",\"path\":\"/a\",\"statusCode\":200,\"failureEnabled\":\"yes\"}")).contains("failureEnabled must be true or false"));
    assertTrue(isError(call("update_api_config", "[1]")));
  }

  @Test
  void updateChangesOnlyTheGivenFields() throws Exception {
    ArgumentCaptor<ApiConfigDto> saved = ArgumentCaptor.forClass(ApiConfigDto.class);
    when(apis.findApiConfig("demo", "/orders", "GET")).thenReturn(Optional.of(configWithId(7L)));

    JsonNode r = call("update_api_config", "{\"id\":7,\"statusCode\":503,\"failureEnabled\":true,\"failureStatusCodes\":\"503,500\"}");

    assertFalse(isError(r));
    verify(apis).saveApiConfig(eq("demo"), saved.capture());
    assertEquals(503, saved.getValue().getStatusCode());
    assertTrue(saved.getValue().getFailureEnabled());
    assertEquals("503,500", saved.getValue().getFailureStatusCodes());
    assertEquals("/orders", saved.getValue().getPath());           // untouched
    assertEquals("{\"ok\":true}", saved.getValue().getResponseBody()); // untouched
  }

  @Test
  void updateWithStaleUpdatedAtIsRefused() throws Exception {
    JsonNode r = call("update_api_config", "{\"id\":7,\"statusCode\":404,\"expectedUpdatedAt\":\"2026-10-28T09:00:00\"}");

    assertTrue(isError(r));
    assertTrue(errorText(r).contains("changed since you read it"));
    assertTrue(errorText(r).contains("2026-10-29T10:00:00"));
    verify(apis, never()).saveApiConfig(any(), any());
  }

  @Test
  void updateWithCurrentUpdatedAtAndNullClearsDescription() throws Exception {
    stored.setDescription("old");
    when(apis.findApiConfig("demo", "/orders", "GET")).thenReturn(Optional.of(configWithId(7L)));
    ArgumentCaptor<ApiConfigDto> saved = ArgumentCaptor.forClass(ApiConfigDto.class);

    JsonNode r = call("update_api_config", "{\"id\":7,\"description\":null,\"expectedUpdatedAt\":\"2026-10-29T10:00:00\"}");

    assertFalse(isError(r));
    verify(apis).saveApiConfig(eq("demo"), saved.capture());
    assertNull(saved.getValue().getDescription());
  }

  @Test
  void updateMayNotMoveAnEndpointOntoAnotherOne() throws Exception {
    when(apis.findApiConfig("demo", "/orders", "POST")).thenReturn(Optional.of(configWithId(3L)));

    JsonNode r = call("update_api_config", "{\"id\":7,\"method\":\"POST\"}");

    assertTrue(isError(r));
    assertTrue(errorText(r).contains("already exists"));
  }

  @Test
  void listRequestLogsUsesTheExactEndpointPathAndMasksSecrets() throws Exception {
    RequestLogDto log = new RequestLogDto();
    log.setId(5L);
    log.setMethod("GET");
    log.setPath("/api/@demo/orders");
    log.setResponseStatus(200);
    log.setTimestamp(LocalDateTime.of(2026, 10, 29, 10, 0));
    log.setRequestHeaders("{\"Authorization\":\"Bearer secret\",\"Accept\":\"*/*\",\"Cookie\":\"s=1\"}");
    log.setRequestBody("x".repeat(5000));
    when(logs.searchByExactPath(eq("demo"), eq("/api/@demo/orders"), eq("GET"), eq(""), any(Pageable.class)))
        .thenReturn(new PageImpl<>(Collections.singletonList(log), org.springframework.data.domain.PageRequest.of(1, 2), 5));

    JsonNode d = data(call("list_request_logs", "{\"apiId\":7,\"page\":1,\"size\":2}"));

    assertEquals(5, d.get("totalElements").asInt());
    assertEquals(3, d.get("totalPages").asInt());
    JsonNode item = d.at("/items/0");
    assertFalse(item.get("requestHeaders").asText().contains("secret"));
    assertFalse(item.get("requestHeaders").asText().contains("s=1"));
    assertTrue(item.get("requestHeaders").asText().contains("Accept"));
    assertEquals(2000, item.get("requestBody").asText().length());
  }

  @Test
  void listRequestLogsNeedsAnEndpointAndValidFilters() throws Exception {
    assertTrue(errorText(call("list_request_logs", "{}")).contains("Provide apiId or path"));
    assertTrue(errorText(call("list_request_logs", "{\"path\":\"orders\"}")).contains("path must start with /"));
    assertTrue(errorText(call("list_request_logs", "{\"path\":\"/orders\",\"statusClass\":\"9\"}")).contains("statusClass"));
    assertTrue(errorText(call("list_request_logs", "{\"path\":\"/orders\",\"size\":51}")).contains("between 1 and 50"));
  }

  @Test
  void statsDefaultToTheLastSevenDaysAndAreReturnedAsData() throws Exception {
    RequestLogService.RangeStats stats = new RequestLogService.RangeStats();
    stats.total = 10;
    stats.byMethod.put("GET", 7L);
    stats.byStatusClass.put("2xx", 8L);
    stats.byStatusClass.put("5xx", 2L);
    stats.byDay.put("2026-10-29", 10L);
    stats.topPaths.put("/api/@demo/orders", 10L);
    when(logs.statsBetween(eq("demo"), any(LocalDateTime.class), any(LocalDateTime.class), eq(""))).thenReturn(stats);

    JsonNode d = data(call("get_request_log_stats", "{}"));

    assertEquals(10, d.get("total").asInt());
    assertEquals(2, d.get("errorCount").asInt());
    assertEquals(7, d.at("/byMethod/GET").asInt());
    ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(logs).statsBetween(eq("demo"), from.capture(), to.capture(), eq(""));
    assertEquals(7, java.time.Duration.between(from.getValue(), to.getValue()).toDays());
  }

  @Test
  void statsAcceptAnEndpointAndRejectBadRanges() throws Exception {
    when(logs.statsBetween(any(), any(), any(), any())).thenReturn(new RequestLogService.RangeStats());

    call("get_request_log_stats", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-03\",\"apiId\":7}");
    verify(logs).statsBetween(eq("demo"), eq(LocalDateTime.of(2026, 10, 1, 0, 0)), eq(LocalDateTime.of(2026, 10, 4, 0, 0)), eq("/api/@demo/orders"));

    assertTrue(errorText(call("get_request_log_stats", "{\"from\":\"2026-10-05\",\"to\":\"2026-10-01\"}")).contains("before"));
    assertTrue(errorText(call("get_request_log_stats", "{\"from\":\"2026-01-01\",\"to\":\"2026-10-01\"}")).contains("limited to 90"));
    assertTrue(errorText(call("get_request_log_stats", "{\"from\":\"yesterday\"}")).contains("must be a date"));
  }

  @Test
  void everyToolCallIsRecordedWithTheOutcome() throws Exception {
    call("get_api_config", "{\"id\":7}");
    call("get_api_config", "{\"id\":\"x\"}");

    ArgumentCaptor<Boolean> ok = ArgumentCaptor.forClass(Boolean.class);
    ArgumentCaptor<McpService.Caller> caller = ArgumentCaptor.forClass(McpService.Caller.class);
    verify(calls, org.mockito.Mockito.times(2)).record(eq(user), eq("get_api_config"), any(JsonNode.class), ok.capture(), anyString(),
        org.mockito.ArgumentMatchers.anyLong(), caller.capture());
    assertEquals(List.of(true, false), ok.getAllValues());
    assertEquals("claude-code", caller.getValue().client);
    assertEquals("k3Fz", caller.getValue().tokenSuffix);
  }

  @Test
  void aFailingActivityLogNeverBreaksTheTool() throws Exception {
    org.mockito.Mockito.doThrow(new RuntimeException("db down")).when(calls)
        .record(any(), anyString(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), org.mockito.ArgumentMatchers.anyLong(), any());

    assertFalse(isError(call("get_api_config", "{\"id\":7}")));
  }

  @Test
  void unknownToolsAreNotRecorded() throws Exception {
    call("delete_everything", "{}");

    verify(calls, never()).record(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
  }

  private ApiConfig configWithId(long id) {
    ApiConfig c = new ApiConfig();
    c.setId(id);
    return c;
  }
}
