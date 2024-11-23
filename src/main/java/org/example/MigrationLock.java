package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;

public class MigrationLock {

    private static final Logger logger = LoggerFactory.getLogger(MigrationLock.class); // Logger instance
    private static final String url = PropertiesUtils.getDbUrl();

    public static void ensureLockTableExists() {

        String createTableSQL = "CREATE TABLE IF NOT EXISTS migration_lock (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " + // Ensures only one row can exist
                "is_locked BOOLEAN NOT NULL DEFAULT 0, " +
                "locked_by VARCHAR(100), " +
                "locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            logger.info("Table 'migration_lock' has been created or already exists.");

        } catch (Exception e) {
            logger.error("Error creating 'migration_lock' table: {}", e.getMessage(), e);
        }
    }

    public static boolean acquireLock() {
        String username = System.getProperty("user.name");

        String selectQuery = "SELECT is_locked, locked_at FROM migration_lock WHERE id = 1";
        String lockQuery = "INSERT INTO migration_lock (id, is_locked, locked_by, locked_at) " +
                "VALUES (1, 1, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (id) DO UPDATE SET is_locked = 1, locked_by = ?, locked_at = CURRENT_TIMESTAMP";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
             PreparedStatement lockStmt = conn.prepareStatement(lockQuery)) {

            // Chreck if the lock is aleady set
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("is_locked")) {
                    Timestamp lockedAtTimestamp = rs.getTimestamp("locked_at");
                    if (lockedAtTimestamp != null) {
                        LocalDateTime lockedAt = lockedAtTimestamp.toLocalDateTime();
                        LocalDateTime now = LocalDateTime.now();
                        Duration duration = Duration.between(lockedAt, now);

                        if (duration.toMinutes() < 1) {
                            logger.warn("Migration is already running. Lock is active for less than 1 minute.");
                            return false;
                        } else {
                            logger.info("Lock has been active for more than 1 minute. Proceeding to acquire lock.");
                        }
                    } else {
                        logger.warn("Locked_at timestamp is null. Proceeding to acquire lock.");
                    }
                }
            }

            // Acquire the lock
            lockStmt.setString(1, username); // insert
            lockStmt.setString(2, username); // update
            lockStmt.executeUpdate();
            logger.info("Lock acquired by: {}", username);
            return true;

        } catch (Exception e) {
            logger.error("Error acquiring lock: {}", e.getMessage(), e);
            return false;
        }
    }

    public static void releaseLock() {
        String unlockQuery = "UPDATE migration_lock SET is_locked = 0, locked_by = NULL, locked_at = NULL WHERE id = 1";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement unlockStmt = conn.prepareStatement(unlockQuery)) {

            unlockStmt.executeUpdate();
            logger.info("Lock released.");

        } catch (Exception e) {
            logger.error("Error releasing lock: {}", e.getMessage(), e);
        }
    }
}
