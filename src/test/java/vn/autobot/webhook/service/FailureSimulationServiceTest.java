package vn.autobot.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.autobot.webhook.model.ApiConfig;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Failure simulation: the first N calls of each caller fail, then one success, then a new round
class FailureSimulationServiceTest {

  private FailureSimulationService service;
  private AtomicLong now;
  private ApiConfig config;

  @BeforeEach
  void setUp() {
    service = new FailureSimulationService();
    now = new AtomicLong(1_000_000);
    service.clock = now::get;
    config = new ApiConfig();
    config.setId(1L);
    config.setFailureEnabled(true);
    config.setFailureCount(2);
    config.setFailureStatusCodes("503,500");
    config.setRetryAfterSeconds(5);
  }

  private MockHttpServletRequest from(String ip) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(ip);
    return request;
  }

  @Test
  void failsFirstNCallsThenSucceedsAndStartsAgain() {
    MockHttpServletRequest request = from("1.1.1.1");

    FailureSimulationService.Failure first = service.next(config, request).get();
    FailureSimulationService.Failure second = service.next(config, request).get();
    assertEquals(503, first.status);
    assertEquals(1, first.attempt);
    assertEquals(2, first.total);
    assertEquals(5, first.retryAfterSeconds);
    assertEquals(500, second.status); // codes are used in turn
    assertEquals(2, second.attempt);

    assertFalse(service.next(config, request).isPresent()); // success
    assertTrue(service.next(config, request).isPresent());   // next round fails again
  }

  @Test
  void disabledOrNullMeansAlwaysSuccess() {
    config.setFailureEnabled(false);
    assertFalse(service.next(config, from("1.1.1.1")).isPresent());
    config.setFailureEnabled(null);
    assertFalse(service.next(config, from("1.1.1.1")).isPresent());
  }

  @Test
  void callersAreCountedSeparately() {
    assertEquals(1, service.next(config, from("1.1.1.1")).get().attempt);
    assertEquals(1, service.next(config, from("2.2.2.2")).get().attempt);
    assertEquals(2, service.next(config, from("1.1.1.1")).get().attempt);
  }

  @Test
  void idempotencyKeyBeatsIpAndRequestIdIsNext() {
    MockHttpServletRequest a = from("1.1.1.1");
    a.addHeader("Idempotency-Key", "order-1");
    MockHttpServletRequest b = from("9.9.9.9"); // different IP, same key: same caller
    b.addHeader("Idempotency-Key", "order-1");
    MockHttpServletRequest c = from("1.1.1.1");
    c.addHeader("X-Request-Id", "req-7");

    assertEquals(1, service.next(config, a).get().attempt);
    assertEquals(2, service.next(config, b).get().attempt);
    assertEquals(1, service.next(config, c).get().attempt);
  }

  @Test
  void endpointsAreCountedSeparately() {
    ApiConfig other = new ApiConfig();
    other.setId(2L);
    other.setFailureEnabled(true);
    other.setFailureCount(2);
    MockHttpServletRequest request = from("1.1.1.1");

    service.next(config, request);
    assertEquals(1, service.next(other, request).get().attempt);
  }

  @Test
  void idleCallerStartsOverAfterTtl() {
    MockHttpServletRequest request = from("1.1.1.1");
    service.next(config, request);
    assertEquals(2, service.next(config, request).get().attempt);

    now.addAndGet(FailureSimulationService.STATE_TTL_MS + 1);

    assertEquals(1, service.next(config, request).get().attempt);
  }

  @Test
  void countIsClampedAndMissingValuesFallBackToDefaults() {
    config.setFailureCount(null);
    config.setFailureStatusCodes(null);
    config.setRetryAfterSeconds(null);
    MockHttpServletRequest request = from("1.1.1.1");

    FailureSimulationService.Failure f = service.next(config, request).get();

    assertEquals(3, f.total);
    assertEquals(503, f.status);
    assertEquals(0, f.retryAfterSeconds);

    config.setId(5L);
    config.setFailureCount(99);
    assertEquals(10, service.next(config, request).get().total);
  }

  @Test
  void parseCodesKeepsOnlyErrorCodes() {
    assertEquals(Arrays.asList(500, 429), FailureSimulationService.parseCodes(" 500, 200 ,abc,429,700"));
    assertEquals(Arrays.asList(503), FailureSimulationService.parseCodes(""));
    assertEquals(Arrays.asList(503), FailureSimulationService.parseCodes(null));
  }

  @Test
  void worksWithoutARequest() {
    assertEquals(Optional.of(1), service.next(config, null).map(f -> f.attempt));
  }
}
