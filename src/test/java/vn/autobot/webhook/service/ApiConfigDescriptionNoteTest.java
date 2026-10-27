package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.dto.ApiConfigDto;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.ApiConfigRepository;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Issue #35: description (max 500) and note (max 2000) on API configs
class ApiConfigDescriptionNoteTest {

  private ApiConfigRepository repository;
  private UserService userService;
  private ApiMockService apiMockService;
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @BeforeEach
  void setUp() {
    repository = mock(ApiConfigRepository.class);
    userService = mock(UserService.class);
    User user = new User();
    user.setId(1L);
    when(userService.findByUsername("demo")).thenReturn(user);
    when(repository.save(any(ApiConfig.class))).thenAnswer(inv -> inv.getArgument(0));
    apiMockService = new ApiMockService(repository, userService, new ObjectMapper());
  }

  private ApiConfigDto dto(String description, String note) {
    ApiConfigDto dto = new ApiConfigDto();
    dto.setPath("/orders");
    dto.setMethod("GET");
    dto.setContentType("application/json");
    dto.setStatusCode(200);
    dto.setDescription(description);
    dto.setNote(note);
    return dto;
  }

  @Test
  void saveTrimsValues() {
    apiMockService.saveApiConfig("demo", dto("  Returns an order  ", " check out flow "));

    ArgumentCaptor<ApiConfig> saved = ArgumentCaptor.forClass(ApiConfig.class);
    verify(repository).save(saved.capture());
    assertEquals("Returns an order", saved.getValue().getDescription());
    assertEquals("check out flow", saved.getValue().getNote());
  }

  @Test
  void saveStoresBlankAsNull() {
    apiMockService.saveApiConfig("demo", dto("   ", ""));

    ArgumentCaptor<ApiConfig> saved = ArgumentCaptor.forClass(ApiConfig.class);
    verify(repository).save(saved.capture());
    assertNull(saved.getValue().getDescription());
    assertNull(saved.getValue().getNote());
  }

  @Test
  void validationRejectsTooLongValues() {
    assertTrue(validator.validate(dto("x".repeat(500), "y".repeat(2000))).isEmpty());
    assertEquals(1, validator.validate(dto("x".repeat(501), null)).size());
    assertEquals(1, validator.validate(dto(null, "y".repeat(2001))).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void swaggerSummaryUsesDescriptionWithFallback() {
    ApiMockService mocked = Mockito.mock(ApiMockService.class);
    ApiConfigDto withDescription = dto("Returns an order", "internal");
    ApiConfigDto without = dto(null, "only a note");
    without.setPath("/plain");
    when(mocked.getApiConfigs("demo")).thenReturn(List.of(withDescription, without));
    AppConfig appConfig = new AppConfig();

    Map<String, Object> spec = new SwaggerService(mocked, new ObjectMapper(), new SettingsService(mock(vn.autobot.webhook.repository.AppSettingRepository.class), appConfig)).generateSwaggerSpec("demo");
    Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

    Map<String, Object> orders = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/@demo/orders")).get("get");
    Map<String, Object> plain = (Map<String, Object>) ((Map<String, Object>) paths.get("/api/@demo/plain")).get("get");
    assertEquals("Returns an order", orders.get("summary"));
    assertEquals("Mock API for /api/@demo/plain", plain.get("summary"));
  }
}
