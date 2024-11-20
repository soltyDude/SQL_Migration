package org.example;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;

//база будет доступна через 1 мин а не 30 мин не забыть
//надо использовать когда раню миграции и откаты
public class MigrationLock {
    private static final String DB_FILE_PATH = "data/mydatabase.db";
    static String url = "jdbc:sqlite:" + DB_FILE_PATH;
    public static void ensureLockTableExists() {

        String createTableSQL = "CREATE TABLE IF NOT EXISTS migration_lock (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1), " + // Ensures only one row can exist
                "is_locked BOOLEAN NOT NULL DEFAULT 0, " +
                "locked_by VARCHAR(100), " +
                "locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            System.out.println("Table 'migration_lock' has been created or already exists.");

        } catch (Exception e) {
            System.err.println("Error creating 'migration_lock' table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean acquireLock(String username) {
        String selectQuery = "SELECT is_locked, locked_at FROM migration_lock WHERE id = 1\n";
        String lockQuery = "INSERT INTO migration_lock (id, is_locked, locked_by, locked_at) " +
                "VALUES (1, 1, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (id) DO UPDATE SET is_locked = 1, locked_by = ?, locked_at = CURRENT_TIMESTAMP";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
             PreparedStatement lockStmt = conn.prepareStatement(lockQuery)) {

            // Check if the lock is already set
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("is_locked")) {
                    Timestamp lockedAtTimestamp = rs.getTimestamp("locked_at");
                    if (lockedAtTimestamp != null) {
                        LocalDateTime lockedAt = lockedAtTimestamp.toLocalDateTime();
                        LocalDateTime now = LocalDateTime.now();
                        Duration duration = Duration.between(lockedAt, now);

                        if (duration.toMinutes() < 1) {
                            System.err.println("Migration is already running. Lock is active for less than 30 minutes.");
                            return false;
                        } else {
                            System.out.println("Lock has been active for more than 30 minutes. Proceeding to acquire lock.");
                        }
                    } else {
                        System.out.println("Locked_at timestamp is null. Proceeding to acquire lock.");
                    }
                }
            }

            // Acquire the lock
            //insert
            lockStmt.setString(1, username);
            //update
            lockStmt.setString(2, username);
            lockStmt.executeUpdate();
            System.out.println("Lock acquired by: " + username);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void releaseLock() {
        String unlockQuery = "UPDATE migration_lock SET is_locked = 0, locked_by = NULL, locked_at = NULL WHERE id = 1";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement unlockStmt = conn.prepareStatement(unlockQuery)) {

            unlockStmt.executeUpdate();
            System.out.println("Lock released.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
