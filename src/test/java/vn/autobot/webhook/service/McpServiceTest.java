package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.model.User;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// MCP JSON-RPC handling: lifecycle, tool listing and the list_api_configs tool
class McpServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private ApiMockService apiMockService;
  private McpService service;
  private User user;

  @BeforeEach
  void setUp() {
    apiMockService = mock(ApiMockService.class);
    SettingsService settings = mock(SettingsService.class);
    when(settings.getDomain()).thenReturn("https://mock.test");
    when(settings.getMcpMaxPageSize()).thenReturn(50);
    service = new McpService(apiMockService, mock(RequestLogService.class), mock(McpCallService.class), settings,
        javax.validation.Validation.buildDefaultValidatorFactory().getValidator(), mapper);
    user = new User();
    user.setUsername("demo");
  }

  private JsonNode send(String json) throws Exception {
    return service.handle(mapper.readTree(json), user).map(n -> (JsonNode) n).orElse(null);
  }

  private ApiConfigDto config(long id, String path) {
    ApiConfigDto dto = new ApiConfigDto();
    dto.setId(id);
    dto.setMethod("GET");
    dto.setPath(path);
    dto.setStatusCode(200);
    dto.setContentType("application/json");
    dto.setDelayMs(0);
    dto.setFailureEnabled(id % 2 == 0);
    return dto;
  }

  @Test
  void initializeEchoesASupportedVersionAndAdvertisesTools() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\"}}");

    assertEquals("2.0", r.get("jsonrpc").asText());
    assertEquals(1, r.get("id").asInt());
    assertEquals("2025-03-26", r.at("/result/protocolVersion").asText());
    assertTrue(r.at("/result/capabilities/tools").isObject());
    assertEquals("webhook-mockapi", r.at("/result/serverInfo/name").asText());
  }

  @Test
  void initializeOffersTheNewestVersionForAnUnknownOne() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":\"a\",\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"1999-01-01\"}}");

    assertEquals("2025-06-18", r.at("/result/protocolVersion").asText());
    assertEquals("a", r.get("id").asText()); // string ids are kept
  }

  @Test
  void notificationsAndClientResponsesGetNoAnswer() throws Exception {
    assertEquals(null, send("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));
    assertEquals(null, send("{\"jsonrpc\":\"2.0\",\"method\":\"whatever/unknown\"}"));
    assertEquals(null, send("{\"jsonrpc\":\"2.0\",\"id\":5,\"result\":{}}"));
  }

  @Test
  void pingReturnsAnEmptyResult() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}");

    assertTrue(r.get("result").isObject());
    assertEquals(0, r.get("result").size());
  }

  @Test
  void unknownMethodIsAJsonRpcError() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"resources/list\"}");

    assertEquals(McpService.METHOD_NOT_FOUND, r.at("/error/code").asInt());
    assertEquals(3, r.get("id").asInt());
  }

  @Test
  void invalidMessagesAreRejected() throws Exception {
    assertEquals(McpService.INVALID_REQUEST, send("[1,2]").at("/error/code").asInt()); // batches are not supported
    assertEquals(McpService.INVALID_REQUEST, send("{\"jsonrpc\":\"2.0\",\"id\":1}").at("/error/code").asInt());
    assertEquals(McpService.INVALID_REQUEST, send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":5}").at("/error/code").asInt());
    assertTrue(send("[1]").get("id").isNull());
    assertEquals(McpService.PARSE_ERROR, service.parseError().at("/error/code").asInt());
  }

  @Test
  void toolsListDescribesListApiConfigs() throws Exception {
    JsonNode tool = send("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\"}").at("/result/tools/0");

    assertEquals("list_api_configs", tool.get("name").asText());
    assertEquals("object", tool.at("/inputSchema/type").asText());
    assertEquals(50, tool.at("/inputSchema/properties/size/maximum").asInt());
  }

  @Test
  void listApiConfigsPagesNewestFirstAndBuildsUrls() throws Exception {
    List<ApiConfigDto> all = new ArrayList<>();
    for (long i = 1; i <= 5; i++) {
      all.add(config(i, "/p" + i));
    }
    when(apiMockService.getApiConfigs("demo")).thenReturn(all);

    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"list_api_configs\",\"arguments\":{\"page\":1,\"size\":2}}}");
    assertFalse(r.at("/result/isError").asBoolean());
    JsonNode data = mapper.readTree(r.at("/result/content/0/text").asText());

    assertEquals(5, data.get("totalElements").asInt());
    assertEquals(3, data.get("totalPages").asInt());
    assertEquals(2, data.get("items").size());
    assertEquals(3, data.at("/items/0/id").asInt()); // page 0 = ids 5,4; page 1 = ids 3,2
    assertEquals("https://mock.test/api/@demo/p3", data.at("/items/0/url").asText());
    assertFalse(data.at("/items/0/failureSimulation").asBoolean());
    assertTrue(data.at("/items/1/failureSimulation").asBoolean());
  }

  @Test
  void defaultsApplyWhenNoArgumentsAreGiven() throws Exception {
    when(apiMockService.getApiConfigs("demo")).thenReturn(new ArrayList<>(List.of(config(1, "/a"))));

    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"list_api_configs\"}}");
    JsonNode data = mapper.readTree(r.at("/result/content/0/text").asText());

    assertEquals(0, data.get("page").asInt());
    assertEquals(20, data.get("size").asInt());
    assertEquals(1, data.get("items").size());
  }

  @Test
  void badArgumentsAreAToolErrorNotAProtocolError() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\",\"params\":{\"name\":\"list_api_configs\",\"arguments\":{\"size\":500}}}");

    assertTrue(r.at("/result/isError").asBoolean());
    assertTrue(r.at("/result/content/0/text").asText().contains("size"));
  }

  @Test
  void unknownToolIsInvalidParams() throws Exception {
    JsonNode r = send("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"delete_everything\"}}");

    assertEquals(McpService.INVALID_PARAMS, r.at("/error/code").asInt());
  }
}
