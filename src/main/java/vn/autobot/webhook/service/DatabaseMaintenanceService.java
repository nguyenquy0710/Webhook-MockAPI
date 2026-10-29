package vn.autobot.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * H2 maintenance: CHECKPOINT (flush and trim the transaction log) and ANALYZE (refresh query statistics). On a database
 * that was never compacted it also schedules SHUTDOWN COMPACT for the next startup (see StartupDatabaseCompactor), which
 * rewrites the file to reclaim space left by deleted rows and needs free disk space about the size of the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMaintenanceService {

    private static final String COMPACTED = "maintenance.compacted";
    private static final String COMPACT_PENDING = "maintenance.compactPending";
    private static final String LAST_RUN_AT = "maintenance.lastRunAt";
    private static final String LAST_STATUS = "maintenance.lastStatus";
    private static final String LAST_SUMMARY = "maintenance.lastSummary";

    private final JdbcTemplate jdbcTemplate;
    private final SettingsService settings;
    private final RequestLogService requestLogService;
    private final McpCallService mcpCallService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public static class Status {
        public final String lastRunAt;
        public final String lastStatus;
        public final String lastSummary;
        public final long sizeBytes;
        public final boolean compacted;
        public final boolean compactPending;
        public final boolean running;

        Status(String lastRunAt, String lastStatus, String lastSummary, long sizeBytes, boolean compacted, boolean compactPending, boolean running) {
            this.lastRunAt = lastRunAt;
            this.lastStatus = lastStatus;
            this.lastSummary = lastSummary;
            this.sizeBytes = sizeBytes;
            this.compacted = compacted;
            this.compactPending = compactPending;
            this.running = running;
        }

        public String getLastRunAt() { return lastRunAt; }
        public String getLastStatus() { return lastStatus; }
        public String getLastSummary() { return lastSummary; }
        public long getSizeBytes() { return sizeBytes; }
        public boolean isCompacted() { return compacted; }
        public boolean isCompactPending() { return compactPending; }
        public boolean isRunning() { return running; }
    }

    public Status getStatus() {
        return new Status(
                settings.get(LAST_RUN_AT).orElse(null),
                settings.get(LAST_STATUS).orElse(null),
                settings.get(LAST_SUMMARY).orElse(null),
                dbFile().length(),
                settings.get(COMPACTED).map(Boolean::parseBoolean).orElse(false),
                settings.get(COMPACT_PENDING).map(Boolean::parseBoolean).orElse(false),
                running.get());
    }

    /** Runs once a day (03:30 server time); a manual run from Admin > Settings uses the same code. */
    @Scheduled(cron = "0 30 3 * * *")
    public void runDaily() {
        run();
    }

    /** @return false if a run is already in progress */
    public boolean run() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        long start = System.currentTimeMillis();
        long before = dbFile().length();
        StringBuilder done = new StringBuilder();
        String status = "success";
        try {
            int retentionDays = settings.getRequestLogRetentionDays();
            if (retentionDays > 0) {
                done.append("Deleted ").append(requestLogService.purgeOlderThan(retentionDays))
                        .append(" request log(s) older than ").append(retentionDays).append(" days, ");
            }
            int oldCalls = mcpCallService.purgeOld();
            if (oldCalls > 0) {
                done.append("Deleted ").append(oldCalls).append(" MCP call(s) older than ").append(McpCallService.RETENTION_DAYS).append(" days, ");
            }
            jdbcTemplate.execute("CHECKPOINT");
            done.append("Checkpoint");
            jdbcTemplate.execute("ANALYZE");
            done.append(", Analyze");
            boolean compacted = settings.get(COMPACTED).map(Boolean::parseBoolean).orElse(false);
            if (!compacted && !settings.get(COMPACT_PENDING).map(Boolean::parseBoolean).orElse(false)) {
                // H2 can only compact the file while no connection pool is open, so it is done at the next restart
                settings.set(COMPACT_PENDING, "true");
                done.append("; file compaction scheduled for the next restart");
            }
        } catch (Exception e) {
            log.error("Database maintenance failed", e);
            status = "error";
            done.append(" failed: ").append(e.getMessage());
        } finally {
            running.set(false);
        }
        long after = dbFile().length();
        settings.set(LAST_RUN_AT, Instant.now().toString());
        settings.set(LAST_STATUS, status);
        settings.set(LAST_SUMMARY, done + " (" + (System.currentTimeMillis() - start) + " ms, "
                + before / 1024 + " KB -> " + after / 1024 + " KB)");
        return true;
    }

    private File dbFile() {
        String path = datasourceUrl.replaceFirst("^jdbc:h2:(file:)?", "");
        int options = path.indexOf(';');
        if (options >= 0) {
            path = path.substring(0, options);
        }
        return new File(path + ".mv.db");
    }
}
