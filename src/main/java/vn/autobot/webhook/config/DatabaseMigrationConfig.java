package vn.autobot.webhook.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Database migration configuration and monitoring component.
 * This class helps manage database migrations and provides logging
 * for migration status and issues.
 */
@Component
@Slf4j
public class DatabaseMigrationConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    /**
     * Logs migration information after application startup.
     * This provides users with visibility into the migration process.
     */
    @EventListener
    public void handleApplicationStarted(ApplicationStartedEvent event) {
        try {
            var info = flyway.info();
            var current = info.current();
            var pending = info.pending();
            
            if (current != null) {
                log.info("Current database version: {}", current.getVersion());
                log.info("Database migration status: {}", current.getState());
            } else {
                log.info("Database is at baseline version");
            }
            
            if (pending.length > 0) {
                log.warn("There are {} pending migrations. Application may have issues.", pending.length);
                for (var migration : pending) {
                    log.warn("Pending migration: {} - {}", migration.getVersion(), migration.getDescription());
                }
            } else {
                log.info("Database is up to date. All migrations have been applied.");
            }
            
        } catch (Exception e) {
            log.error("Error checking database migration status: {}", e.getMessage());
        }
    }
}