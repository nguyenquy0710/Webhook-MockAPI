package vn.autobot.webhook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.service.McpService;
import vn.autobot.webhook.service.McpTokenService;
import vn.autobot.webhook.service.SettingsService;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// HTTP behaviour of /mcp/@{username}/mcp: auth, Origin, protocol version, status codes
class McpControllerTest {

  private static final String PING = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
  private static final String URL = "/mcp/@demo/mcp";

  private MockMvc mvc;
  private SettingsService settings;
  private vn.autobot.webhook.service.McpSessionRegistry sessions;

  @BeforeEach
  void setUp() {
    McpTokenService tokens = mock(McpTokenService.class);
    User demo = new User();
    demo.setId(1L);
    demo.setUsername("demo");
    User other = new User();
    other.setId(2L);
    other.setUsername("other");
    when(tokens.authenticate("mcp_good")).thenReturn(Optional.of(demo));
    when(tokens.authenticate("mcp_other")).thenReturn(Optional.of(other));
    when(tokens.authenticate("mcp_bad")).thenReturn(Optional.empty());
    settings = mock(SettingsService.class);
    when(settings.getDomain()).thenReturn("https://mock.example.test");
    when(settings.isMcpEnabled()).thenReturn(true);
    when(settings.getMcpRateLimitPerMinute()).thenReturn(60);
    when(settings.getMcpMaxPageSize()).thenReturn(50);
    when(settings.getMcpExtraOrigins()).thenReturn(java.util.Collections.emptyList());
    ObjectMapper mapper = new ObjectMapper();
    McpService service = new McpService(mock(vn.autobot.webhook.service.ApiMockService.class), mock(vn.autobot.webhook.service.RequestLogService.class),
        mock(vn.autobot.webhook.service.McpCallService.class), settings,
        javax.validation.Validation.buildDefaultValidatorFactory().getValidator(), mapper);
    sessions = new vn.autobot.webhook.service.McpSessionRegistry();
    mvc = MockMvcBuilders.standaloneSetup(new McpController(tokens, service, settings, new vn.autobot.webhook.service.McpRateLimiter(), sessions, mapper)).build();
  }

  @Test
  void pingWithAValidTokenReturnsJson() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.result").isMap());
  }

  @Test
  void bearerSchemeIsCaseInsensitive() throws Exception {
    mvc.perform(post(URL).header("Authorization", "bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk());
  }

  @Test
  void missingOrWrongTokenIs401WithChallenge() throws Exception {
    mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer realm=\"mcp\""));
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_bad").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isUnauthorized());
    mvc.perform(post(URL).header("Authorization", "Basic abc").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void tokenOfAnotherUserIs403() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_other").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isForbidden());
  }

  @Test
  void notificationsGet202WithoutBody() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON)
            .content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))
        .andExpect(status().isAccepted())
        .andExpect(content().string(""));
  }

  @Test
  void brokenJsonIs400WithParseError() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content("{oops"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value(-32700));
  }

  @Test
  void getAndDeleteAreNotAllowed() throws Exception {
    mvc.perform(get(URL)).andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", "POST"));
    mvc.perform(delete(URL)).andExpect(status().isMethodNotAllowed());
  }

  @Test
  void unsupportedProtocolVersionHeaderIs400() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("MCP-Protocol-Version", "1999-01-01")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isBadRequest());
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("MCP-Protocol-Version", "2025-06-18")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk());
  }

  @Test
  void originMustBeThisHostOrThePublicOne() throws Exception {
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "https://evil.example.com")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isForbidden());
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "https://mock.example.test")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk());
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "http://localhost")
            .contentType(MediaType.APPLICATION_JSON).content(PING)) // MockMvc serves localhost
        .andExpect(status().isOk());
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "not a uri")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isForbidden());
  }

  @Test
  void switchedOffMcpIs503() throws Exception {
    when(settings.isMcpEnabled()).thenReturn(false);

    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void rateLimitIs429WithRetryAfter() throws Exception {
    when(settings.getMcpRateLimitPerMinute()).thenReturn(2);

    for (int i = 0; i < 2; i++) {
      mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
          .andExpect(status().isOk());
    }
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void extraOriginsFromSettingsAreAllowed() throws Exception {
    when(settings.getMcpExtraOrigins()).thenReturn(java.util.Collections.singletonList("http://localhost:6274"));

    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "http://localhost:6274")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk());
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "http://localhost:9999")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isOk()); // MockMvc serves localhost, so this one passes as the same host
    mvc.perform(post(URL).header("Authorization", "Bearer mcp_good").header("Origin", "https://other.example.com")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isForbidden());
  }

  @Test
  void sseAnnouncesTheMessageEndpointAndAnswersArriveOnTheStream() throws Exception {
    org.springframework.test.web.servlet.MvcResult stream = mvc.perform(get("/mcp/@demo/sse").header("Authorization", "Bearer mcp_good"))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
        .andReturn();
    String opening = stream.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(opening.contains("event:endpoint"), opening);
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("data:(/mcp/@demo/message\\?sessionId=[0-9a-f-]+)").matcher(opening);
    org.junit.jupiter.api.Assertions.assertTrue(m.find(), opening);
    String endpoint = m.group(1);

    mvc.perform(post(endpoint).header("Authorization", "Bearer mcp_good").contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isAccepted())
        .andExpect(content().string(""));

    String afterwards = stream.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(afterwards.contains("event:message"), afterwards);
    org.junit.jupiter.api.Assertions.assertTrue(afterwards.contains("\"id\":1"), afterwards);
  }

  @Test
  void sseNeedsAValidTokenOfTheRightUser() throws Exception {
    mvc.perform(get("/mcp/@demo/sse")).andExpect(status().isUnauthorized());
    mvc.perform(get("/mcp/@demo/sse").header("Authorization", "Bearer mcp_other")).andExpect(status().isForbidden());
  }

  @Test
  void messagesNeedAnOpenSessionOfTheSameUser() throws Exception {
    mvc.perform(post("/mcp/@demo/message?sessionId=nope").header("Authorization", "Bearer mcp_good")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isNotFound());
    mvc.perform(post("/mcp/@demo/message").header("Authorization", "Bearer mcp_good")
            .contentType(MediaType.APPLICATION_JSON).content(PING))
        .andExpect(status().isNotFound());
  }

  @Test
  void tooManyStreamsAreRefused() throws Exception {
    for (int i = 0; i < 5; i++) {
      mvc.perform(get("/mcp/@demo/sse").header("Authorization", "Bearer mcp_good"))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted());
    }
    mvc.perform(get("/mcp/@demo/sse").header("Authorization", "Bearer mcp_good"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "30"));
  }
}
