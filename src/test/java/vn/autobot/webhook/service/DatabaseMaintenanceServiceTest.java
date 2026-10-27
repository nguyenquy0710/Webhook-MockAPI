package vn.autobot.webhook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.model.AppSetting;
import vn.autobot.webhook.repository.AppSettingRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Admin > Settings > Database maintenance: what a run does and records
class DatabaseMaintenanceServiceTest {

  @TempDir
  Path dir;

  private JdbcTemplate jdbc;
  private RequestLogService logs;
  private SettingsService settings;
  private DatabaseMaintenanceService service;

  @BeforeEach
  void setUp() throws Exception {
    Files.write(dir.resolve("db.mv.db"), new byte[2048]);
    jdbc = mock(JdbcTemplate.class);
    logs = mock(RequestLogService.class);
    AppSettingRepository repo = mock(AppSettingRepository.class);
    when(repo.findById(anyString())).thenReturn(Optional.empty());
    when(repo.save(any(AppSetting.class))).thenAnswer(i -> i.getArgument(0));
    settings = new SettingsService(repo, new AppConfig());
    service = new DatabaseMaintenanceService(jdbc, settings, logs);
    ReflectionTestUtils.setField(service, "datasourceUrl", "jdbc:h2:file:" + dir.resolve("db") + ";DB_CLOSE_DELAY=-1");
  }

  @Test
  void runCheckpointsThenAnalyzesAndRecordsResult() {
    assertTrue(service.run());

    InOrder order = inOrder(jdbc);
    order.verify(jdbc).execute("CHECKPOINT");
    order.verify(jdbc).execute("ANALYZE");
    verify(jdbc, never()).execute("SHUTDOWN COMPACT"); // never while the pool is open
    DatabaseMaintenanceService.Status status = service.getStatus();
    assertEquals("success", status.getLastStatus());
    assertNotNull(status.getLastRunAt());
    assertEquals(2048, status.getSizeBytes());
  }

  @Test
  void firstRunSchedulesCompactionForNextRestart() {
    service.run();

    assertTrue(service.getStatus().isCompactPending());
    assertFalse(service.getStatus().isCompacted());
    assertTrue(service.getStatus().getLastSummary().contains("compaction scheduled for the next restart"));
  }

  @Test
  void alreadyCompactedDatabaseDoesNotScheduleAgain() {
    settings.set("maintenance.compacted", "true");

    service.run();

    assertFalse(service.getStatus().isCompactPending());
    assertFalse(service.getStatus().getLastSummary().contains("compaction"));
  }

  @Test
  void purgesOldLogsOnlyWhenRetentionIsSet() {
    service.run();
    verify(logs, never()).purgeOlderThan(anyInt());

    settings.set("general.requestLogRetentionDays", "30");
    when(logs.purgeOlderThan(30)).thenReturn(7);
    service.run();

    verify(logs).purgeOlderThan(30);
    assertTrue(service.getStatus().getLastSummary().startsWith("Deleted 7 request log(s) older than 30 days"));
  }

  @Test
  void failureIsRecordedAsError() {
    doThrow(new RuntimeException("disk error")).when(jdbc).execute("CHECKPOINT");

    assertTrue(service.run());

    assertEquals("error", service.getStatus().getLastStatus());
    assertTrue(service.getStatus().getLastSummary().contains("disk error"));
    assertFalse(service.getStatus().isRunning());
  }

  @Test
  void overlappingRunIsRejected() {
    doAnswer(i -> {
      assertFalse(service.run()); // a second run while the first is still in progress
      return null;
    }).when(jdbc).execute("CHECKPOINT");

    assertTrue(service.run());
  }
}
