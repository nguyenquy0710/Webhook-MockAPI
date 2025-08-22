package vn.autobot.webhook.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
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
            checkAndRepairMigrations();
            logMigrationStatus();
        } catch (Exception e) {
            log.error("Error checking database migration status: {}", e.getMessage());
        }
    }

    /**
     * Checks for failed migrations and attempts to repair them.
     */
    private void checkAndRepairMigrations() {
        try {
            var info = flyway.info();
            var all = info.all();
            
            boolean hasFailedMigrations = false;
            for (MigrationInfo migration : all) {
                if (migration.getState() == MigrationState.FAILED) {
                    log.error("Found failed migration: {} - {}", 
                        migration.getVersion(), migration.getDescription());
                    hasFailedMigrations = true;
                }
            }
            
            if (hasFailedMigrations) {
                log.warn("Attempting to repair failed migrations...");
                flyway.repair();
                log.info("Migration repair completed successfully");
            }
        } catch (FlywayException e) {
            log.error("Error during migration repair: {}", e.getMessage());
            log.info("You may need to manually resolve migration issues");
        }
    }

    /**
     * Logs current migration status.
     */
    private void logMigrationStatus() {
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