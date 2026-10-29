package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.DeferredResult;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.ApiConfigRepository;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// What a client sees from an endpoint with failure simulation on, plus how the settings are saved and validated
class ApiMockFailureSimulationTest {

  private ApiConfigRepository repository;
  private ApiMockService service;
  private ApiConfig config;

  @BeforeEach
  void setUp() {
    repository = mock(ApiConfigRepository.class);
    when(repository.save(any(ApiConfig.class))).thenAnswer(i -> i.getArgument(0));
    UserService userService = mock(UserService.class);
    User user = new User();
    user.setId(1L);
    when(userService.findByUsername("demo")).thenReturn(user);
    service = new ApiMockService(repository, userService, new ObjectMapper(), new FailureSimulationService());

    config = new ApiConfig();
    config.setId(7L);
    config.setPath("/pay");
    config.setMethod("POST");
    config.setStatusCode(201);
    config.setContentType("application/json");
    config.setResponseBody("{\"ok\":true}");
    config.setDelayMs(0);
    config.setFailureEnabled(true);
    config.setFailureCount(2);
    config.setFailureStatusCodes("503,500");
    config.setRetryAfterSeconds(2);
  }

  private ResponseEntity<?> call(String key) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/@demo/pay");
    request.addHeader("Idempotency-Key", key);
    DeferredResult<ResponseEntity<Object>> result = service.processWebhook(config, request);
    return (ResponseEntity<?>) result.getResult();
  }

  @Test
  void clientSeesErrorsThenTheConfiguredResponse() {
    ResponseEntity<?> first = call("k1");
    assertEquals(503, first.getStatusCodeValue());
    assertEquals("1/2", first.getHeaders().getFirst("X-Mock-Failure-Attempt"));
    assertEquals("2", first.getHeaders().getFirst("Retry-After"));
    assertTrue(first.getBody().toString().contains("Simulated failure"));

    ResponseEntity<?> second = call("k1");
    assertEquals(500, second.getStatusCodeValue());
    assertNull(second.getHeaders().getFirst("Retry-After")); // only 429/503 carry it

    ResponseEntity<?> third = call("k1");
    assertEquals(201, third.getStatusCodeValue());
    assertNull(third.getHeaders().getFirst("X-Mock-Failure-Attempt"));
  }

  @Test
  void switchedOffEndpointAlwaysAnswersNormally() {
    config.setFailureEnabled(false);
    assertEquals(201, call("k1").getStatusCodeValue());
    assertEquals(201, call("k1").getStatusCodeValue());
  }

  @Test
  void saveStoresSettingsAndAppliesDefaults() {
    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath("/pay");
    dto.setMethod("POST");
    dto.setContentType("application/json");
    dto.setStatusCode(200);
    dto.setFailureEnabled(true);
    dto.setFailureCount(4);
    dto.setFailureStatusCodes(" 429, 503 ");
    dto.setRetryAfterSeconds(3);

    ApiConfig saved = service.saveApiConfig("demo", dto);

    assertTrue(saved.getFailureEnabled());
    assertEquals(4, saved.getFailureCount());
    assertEquals("429,503", saved.getFailureStatusCodes());
    assertEquals(3, saved.getRetryAfterSeconds());

    ApiConfigDto bare = new ApiConfigDto();
    bare.setPath("/x");
    bare.setMethod("GET");
    bare.setContentType("text/plain");
    bare.setStatusCode(200);
    ApiConfig defaults = service.saveApiConfig("demo", bare);
    assertFalse(defaults.getFailureEnabled());
    assertEquals(3, defaults.getFailureCount());
    assertEquals("503", defaults.getFailureStatusCodes());
    assertEquals(1, defaults.getRetryAfterSeconds());
  }

  @Test
  void validationRejectsOutOfRangeValues() {
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    ApiConfigDto ok = new ApiConfigDto();
    ok.setPath("/p");
    ok.setMethod("GET");
    ok.setContentType("text/plain");
    ok.setStatusCode(200);
    ok.setFailureCount(10);
    ok.setFailureStatusCodes("503, 429");
    ok.setRetryAfterSeconds(60);
    assertTrue(validator.validate(ok).isEmpty());

    ok.setFailureCount(11);
    ok.setFailureStatusCodes("200");
    ok.setRetryAfterSeconds(61);
    assertEquals(3, validator.validate(ok).size());
    ok.setFailureCount(0);
    ok.setFailureStatusCodes("abc");
    ok.setRetryAfterSeconds(-1);
    assertEquals(3, validator.validate(ok).size());
  }

  @Test
  void updatedAtIsExposedAtDatabasePrecision() {
    config.setUser(new User());
    config.getUser().setId(1L);
    config.setUpdatedAt(java.time.LocalDateTime.of(2026, 10, 29, 2, 20, 5, 768_749_257));
    when(repository.findById(7L)).thenReturn(java.util.Optional.of(config));

    String updatedAt = service.getApiConfig("demo", 7L).getUpdatedAt();

    assertEquals("2026-10-29T02:20:05.768749", updatedAt);
  }
}
