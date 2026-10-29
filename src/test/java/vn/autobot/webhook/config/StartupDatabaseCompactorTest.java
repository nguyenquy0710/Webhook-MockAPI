package vn.autobot.webhook.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// SHUTDOWN COMPACT at startup: only when scheduled, only with enough free disk, and it records the outcome
class StartupDatabaseCompactorTest {

  @TempDir
  Path dir;

  private String url() {
    return "jdbc:h2:file:" + dir.resolve("db");
  }

  private void seed(boolean pending) throws Exception {
    try (Connection c = DriverManager.getConnection(url(), "sa", "password"); Statement s = c.createStatement()) {
      s.execute("CREATE TABLE app_settings (setting_key VARCHAR(100) PRIMARY KEY, setting_value VARCHAR(1000))");
      s.execute("CREATE TABLE junk (id INT PRIMARY KEY, payload VARCHAR(2000))");
      s.execute("INSERT INTO junk SELECT x, REPEAT('x', 1500) FROM SYSTEM_RANGE(1, 3000)");
      s.execute("DELETE FROM junk");
      if (pending) {
        s.execute("INSERT INTO app_settings VALUES ('maintenance.compactPending', 'true')");
      }
    }
  }

  private String setting(String key) throws Exception {
    try (Connection c = DriverManager.getConnection(url(), "sa", "password"); Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT setting_value FROM app_settings WHERE setting_key = '" + key + "'")) {
      return r.next() ? r.getString(1) : null;
    }
  }

  private long size() {
    return new File(dir.resolve("db.mv.db").toString()).length();
  }

  // Opening and closing the file moves its size by a few KB; compaction would cut it by far more
  private void assertNotCompacted(long before) {
    assertTrue(size() > before / 2, "file should not be compacted: " + before + " -> " + size());
  }

  @Test
  void compactsWhenScheduledAndClearsTheFlag() throws Exception {
    seed(true);
    long before = size();

    assertTrue(StartupDatabaseCompactor.compactIfPending(url(), "sa", "password", f -> Long.MAX_VALUE));

    assertTrue(size() < before, "file should shrink: " + before + " -> " + size());
    assertEquals("true", setting("maintenance.compacted"));
    assertEquals("false", setting("maintenance.compactPending"));
    assertEquals("success", setting("maintenance.lastStatus"));
  }

  @Test
  void skipsWhenNothingIsScheduled() throws Exception {
    seed(false);
    long before = size();

    assertFalse(StartupDatabaseCompactor.compactIfPending(url(), "sa", "password", f -> Long.MAX_VALUE));

    assertNotCompacted(before);
    assertNull(setting("maintenance.lastStatus"));
  }

  @Test
  void skipsAndWarnsWhenDiskIsTooSmall() throws Exception {
    seed(true);
    long before = size();

    assertFalse(StartupDatabaseCompactor.compactIfPending(url(), "sa", "password", f -> 0L));

    assertNotCompacted(before);
    assertEquals("true", setting("maintenance.compactPending")); // retried at the next start
    assertEquals("warning", setting("maintenance.lastStatus"));
    assertTrue(setting("maintenance.lastSummary").contains("needs"));
  }

  @Test
  void missingDatabaseOrSettingsTableIsIgnored() throws Exception {
    assertFalse(StartupDatabaseCompactor.compactIfPending(url(), "sa", "password", f -> Long.MAX_VALUE)); // no file yet

    try (Connection c = DriverManager.getConnection(url(), "sa", "password")) {
      c.createStatement().execute("CREATE TABLE other (id INT)");
    }
    assertFalse(StartupDatabaseCompactor.compactIfPending(url(), "sa", "password", f -> Long.MAX_VALUE)); // no app_settings
  }
}
