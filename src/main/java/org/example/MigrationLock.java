package org.example;

import com.sun.jdi.connect.spi.Connection;

import java.sql.DriverManager;
import java.sql.Statement;

public class MigrationLock {

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
}
