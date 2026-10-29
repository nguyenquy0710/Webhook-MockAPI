package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Redaction, rate limiting and the SSE session registry
class McpSupportTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void redactMasksSensitiveKeysAtAnyDepthAndLeavesTheOriginalAlone() throws Exception {
    JsonNode original = mapper.readTree("{\"path\":\"/a\",\"Authorization\":\"Bearer x\",\"nested\":{\"apiKey\":\"k\",\"ok\":1},"
        + "\"list\":[{\"password\":\"p\",\"name\":\"n\"}],\"X-Api-Key\":\"z\",\"tokenValue\":\"t\"}");

    JsonNode r = McpRedactor.redact(original);

    assertEquals("***", r.get("Authorization").asText());
    assertEquals("***", r.at("/nested/apiKey").asText());
    assertEquals(1, r.at("/nested/ok").asInt());
    assertEquals("***", r.at("/list/0/password").asText());
    assertEquals("n", r.at("/list/0/name").asText());
    assertEquals("***", r.get("X-Api-Key").asText());
    assertEquals("***", r.get("tokenValue").asText());
    assertEquals("/a", r.get("path").asText());
    assertEquals("Bearer x", original.get("Authorization").asText());
  }

  @Test
  void truncateKeepsShortTextAndCutsLongText() {
    assertEquals("abc", McpRedactor.truncate("abc", 5));
    assertEquals(null, McpRedactor.truncate(null, 5));
    assertEquals("abcd…", McpRedactor.truncate("abcdefgh", 5));
    assertTrue(McpRedactor.isSensitiveKey("Set-Cookie"));
    assertFalse(McpRedactor.isSensitiveKey("path"));
  }

  @Test
  void rateLimiterAllowsTheLimitThenReportsWhenToRetry() {
    McpRateLimiter limiter = new McpRateLimiter();
    limiter.clock = Clock.fixed(Instant.parse("2026-10-29T10:00:20Z"), ZoneOffset.UTC);

    assertEquals(0, limiter.tryAcquire("u1", 2));
    assertEquals(0, limiter.tryAcquire("u1", 2));
    int retry = limiter.tryAcquire("u1", 2);

    assertTrue(retry >= 39 && retry <= 41, "retry after " + retry);
    assertEquals(0, limiter.tryAcquire("u2", 2)); // other tokens are independent
  }

  @Test
  void rateLimiterStartsAgainInTheNextMinute() {
    McpRateLimiter limiter = new McpRateLimiter();
    limiter.clock = Clock.fixed(Instant.parse("2026-10-29T10:00:20Z"), ZoneOffset.UTC);
    limiter.tryAcquire("u1", 1);
    assertTrue(limiter.tryAcquire("u1", 1) > 0);

    limiter.clock = Clock.fixed(Instant.parse("2026-10-29T10:01:01Z"), ZoneOffset.UTC);

    assertEquals(0, limiter.tryAcquire("u1", 1));
  }

  @Test
  void registryLimitsStreamsPerUserAndForgetsClosedOnes() {
    McpSessionRegistry registry = new McpSessionRegistry();
    McpSessionRegistry.Session first = null;
    for (int i = 0; i < McpSessionRegistry.MAX_PER_USER; i++) {
      McpSessionRegistry.Session s = registry.open(1L, new SseEmitter()).get();
      first = first == null ? s : first;
    }

    assertFalse(registry.open(1L, new SseEmitter()).isPresent());
    assertTrue(registry.open(2L, new SseEmitter()).isPresent()); // another user is fine
    assertTrue(registry.find(first.id).isPresent());
    assertFalse(registry.find("nope").isPresent());
    assertFalse(registry.find(null).isPresent());
  }
}
