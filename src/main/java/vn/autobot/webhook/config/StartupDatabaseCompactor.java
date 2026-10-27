package vn.autobot.webhook.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.function.ToLongFunction;

/**
 * Compacts the H2 file when Admin > Settings scheduled it (maintenance.compactPending = true).
 * H2 can only run SHUTDOWN COMPACT safely while no connection pool is open, so this runs before the DataSource
 * is created (registered in META-INF/spring.factories). Needs free disk space about the size of the database.
 */
@Slf4j
public class StartupDatabaseCompactor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        if (url == null || !url.startsWith("jdbc:h2:file:")) {
            return;
        }
        try {
            compactIfPending(url, env.getProperty("spring.datasource.username", ""),
                    env.getProperty("spring.datasource.password", ""), f -> f.getParentFile().getAbsoluteFile().getUsableSpace());
        } catch (Exception e) {
            log.error("Startup database compaction failed", e);
        }
    }

    /** @return true if the file was compacted; usableSpace gives the free bytes on the disk holding the database file */
    static boolean compactIfPending(String url, String user, String password, ToLongFunction<File> usableSpace) throws Exception {
        File file = new File(url.replaceFirst("^jdbc:h2:file:", "").split(";")[0] + ".mv.db");
        if (!file.exists() || !isPending(url, user, password)) {
            return false;
        }
        long before = file.length();
        long free = usableSpace.applyAsLong(file);
        if (free < before) {
            record(url, user, password, "warning", "Compaction skipped at startup: needs " + before / 1024
                    + " KB free but only " + free / 1024 + " KB available", false);
            return false;
        }
        long start = System.currentTimeMillis();
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("SHUTDOWN COMPACT");
        }
        record(url, user, password, "success", "Compact at startup (" + (System.currentTimeMillis() - start) + " ms, "
                + before / 1024 + " KB -> " + file.length() / 1024 + " KB)", true);
        log.info("Database compacted at startup: {} KB -> {} KB", before / 1024, file.length() / 1024);
        return true;
    }

    private static boolean isPending(String url, String user, String password) throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            try (ResultSet t = s.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'APP_SETTINGS'")) {
                t.next();
                if (t.getInt(1) == 0) {
                    return false;
                }
            }
            try (ResultSet r = s.executeQuery("SELECT setting_value FROM app_settings WHERE setting_key = 'maintenance.compactPending'")) {
                return r.next() && "true".equals(r.getString(1));
            }
        }
    }

    private static void record(String url, String user, String password, String status, String summary, boolean compacted) throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            put(c, "maintenance.lastRunAt", Instant.now().toString());
            put(c, "maintenance.lastStatus", status);
            put(c, "maintenance.lastSummary", summary);
            if (compacted) {
                put(c, "maintenance.compacted", "true");
                put(c, "maintenance.compactPending", "false");
            }
        }
    }

    private static void put(Connection c, String key, String value) throws Exception {
        try (PreparedStatement p = c.prepareStatement("MERGE INTO app_settings (setting_key, setting_value) KEY(setting_key) VALUES (?, ?)")) {
            p.setString(1, key);
            p.setString(2, value);
            p.executeUpdate();
        }
    }
}
