package vn.autobot.webhook.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.autobot.webhook.config.AppConfig;
import vn.autobot.webhook.service.RequestLogService;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLogCleanupScheduler {

    private final RequestLogService requestLogService;
    private final AppConfig appConfig;

    /**
     * Scheduled job to clean old request logs.
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldLogs() {
        log.info("Starting scheduled request log cleanup...");
        try {
            int retentionDays = appConfig.getRequestLogRetentionDays();
            int deletedCount = requestLogService.cleanOldRequestLogs(retentionDays);
            log.info("Scheduled cleanup completed. Deleted {} old request logs (retention: {} days)", 
                     deletedCount, retentionDays);
        } catch (Exception e) {
            log.error("Error during scheduled request log cleanup", e);
        }
    }
}
