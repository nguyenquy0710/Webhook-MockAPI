package vn.autobot.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.autobot.webhook.model.McpToken;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.McpTokenRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// One MCP token per user: stored hashed, replaced on renew, validated on use
class McpTokenServiceTest {

  private McpTokenRepository repository;
  private McpTokenService service;
  private User user;
  private McpToken stored;

  @BeforeEach
  void setUp() {
    repository = mock(McpTokenRepository.class);
    UserService userService = mock(UserService.class);
    user = new User();
    user.setId(1L);
    user.setUsername("demo");
    when(userService.findByUsername("demo")).thenReturn(user);
    when(repository.findByUser(user)).thenAnswer(i -> Optional.ofNullable(stored));
    when(repository.save(any(McpToken.class))).thenAnswer(i -> {
      stored = i.getArgument(0);
      return stored;
    });
    when(repository.findByTokenHash(any())).thenAnswer(i ->
        stored != null && stored.getTokenHash().equals(i.getArgument(0)) ? Optional.of(stored) : Optional.empty());
    service = new McpTokenService(repository, userService);
  }

  @Test
  void renewReturnsATokenAndStoresOnlyItsHash() {
    String token = service.renew("demo");

    assertTrue(token.startsWith("mcp_"));
    assertTrue(token.length() > 40);
    assertEquals(McpTokenService.hash(token), stored.getTokenHash());
    assertFalse(stored.getTokenHash().contains(token));
    assertEquals(token.substring(token.length() - 4), stored.getTokenSuffix());
    assertNull(stored.getLastUsedAt());
  }

  @Test
  void renewReplacesTheOldToken() {
    String first = service.renew("demo");
    String second = service.renew("demo");

    assertNotEquals(first, second);
    assertFalse(service.authenticate(first).isPresent());
    assertEquals(user, service.authenticate(second).get());
  }

  @Test
  void authenticateRejectsMissingMalformedAndUnknownTokens() {
    service.renew("demo");

    assertFalse(service.authenticate(null).isPresent());
    assertFalse(service.authenticate("").isPresent());
    assertFalse(service.authenticate("not-a-token").isPresent());
    assertFalse(service.authenticate("mcp_unknown").isPresent());
  }

  @Test
  void lastUsedIsRecordedButThrottled() {
    String token = service.renew("demo");
    Instant start = Instant.parse("2026-10-29T10:00:00Z");
    service.clock = Clock.fixed(start, ZoneOffset.UTC);
    ArgumentCaptor<McpToken> saves = ArgumentCaptor.forClass(McpToken.class);

    service.authenticate(token);
    service.authenticate(token); // within a minute: no second write
    verify(repository, times(2)).save(saves.capture()); // renew + first use
    assertEquals(start.atOffset(ZoneOffset.UTC).toLocalDateTime(), stored.getLastUsedAt());

    service.clock = Clock.fixed(start.plusSeconds(61), ZoneOffset.UTC);
    service.authenticate(token);
    verify(repository, times(3)).save(any(McpToken.class));
  }

  @Test
  void invalidTokenNeverWrites() {
    service.authenticate("mcp_unknown");

    verify(repository, never()).save(any(McpToken.class));
  }
}
