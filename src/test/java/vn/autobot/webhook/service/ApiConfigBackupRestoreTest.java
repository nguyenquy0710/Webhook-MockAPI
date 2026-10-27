package vn.autobot.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.autobot.webhook.model.ApiConfig;
import vn.autobot.webhook.model.User;
import vn.autobot.webhook.repository.ApiConfigRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Issue #35: backup/restore must keep description and note, and still accept old backups without them
class ApiConfigBackupRestoreTest {

  private ApiConfigRepository repository;
  private ApiConfigService service;
  private User user;

  @BeforeEach
  void setUp() {
    repository = mock(ApiConfigRepository.class);
    UserService userService = mock(UserService.class);
    user = new User();
    user.setId(1L);
    when(userService.findByUsername("demo")).thenReturn(user);
    when(repository.save(any(ApiConfig.class))).thenAnswer(inv -> inv.getArgument(0));
    service = new ApiConfigService(repository, userService, new ObjectMapper());
  }

  private ApiConfig config(String description, String note) {
    ApiConfig config = new ApiConfig();
    config.setUser(user);
    config.setPath("/orders");
    config.setMethod("GET");
    config.setContentType("application/json");
    config.setStatusCode(200);
    config.setDelayMs(0);
    config.setDescription(description);
    config.setNote(note);
    return config;
  }

  @Test
  void backupThenRestoreKeepsDescriptionAndNote() {
    when(repository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(config("Returns an order", "checkout flow")));

    String backup = service.backupApiConfigs("demo");
    service.restoreApiConfigs("demo", backup);

    ArgumentCaptor<ApiConfig> saved = ArgumentCaptor.forClass(ApiConfig.class);
    verify(repository, times(1)).save(saved.capture());
    assertEquals("Returns an order", saved.getValue().getDescription());
    assertEquals("checkout flow", saved.getValue().getNote());
  }

  @Test
  void restoreAcceptsOldBackupWithoutNewFields() {
    String oldBackup = "[{\"path\":\"/old\",\"method\":\"GET\",\"contentType\":\"text/plain\",\"statusCode\":200,\"delayMs\":0}]";

    service.restoreApiConfigs("demo", oldBackup);

    ArgumentCaptor<ApiConfig> saved = ArgumentCaptor.forClass(ApiConfig.class);
    verify(repository).save(saved.capture());
    assertEquals("/old", saved.getValue().getPath());
    assertNull(saved.getValue().getDescription());
    assertNull(saved.getValue().getNote());
    // an old backup restores with failure simulation off and the default settings
    assertEquals(false, saved.getValue().getFailureEnabled());
    assertEquals(3, saved.getValue().getFailureCount());
    assertEquals("503", saved.getValue().getFailureStatusCodes());
    assertEquals(1, saved.getValue().getRetryAfterSeconds());
  }

  @Test
  void backupThenRestoreKeepsFailureSimulation() {
    ApiConfig flaky = config(null, null);
    flaky.setFailureEnabled(true);
    flaky.setFailureCount(5);
    flaky.setFailureStatusCodes("429,503");
    flaky.setRetryAfterSeconds(7);
    when(repository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(flaky));

    service.restoreApiConfigs("demo", service.backupApiConfigs("demo"));

    ArgumentCaptor<ApiConfig> saved = ArgumentCaptor.forClass(ApiConfig.class);
    verify(repository).save(saved.capture());
    assertEquals(true, saved.getValue().getFailureEnabled());
    assertEquals(5, saved.getValue().getFailureCount());
    assertEquals("429,503", saved.getValue().getFailureStatusCodes());
    assertEquals(7, saved.getValue().getRetryAfterSeconds());
  }
}
