package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class MigrationManager {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class); // Logger instance

    public static void migrateFiles(String dir) {
        try (Connection conn = ConnectionManager.getConnection()) {
            logger.info("Connected to the database.");

            // Initialize the database schema history table if it doesn't exist
            _init_();
            MigrationLock.ensureLockTableExists();

            File folder = new File(dir);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.error("Invalid directory: {}", dir);
                return;
            }
//
            // Find the latest version in the database
            String lastDbVersion = getLastDbVersion(conn);

            // Get a list of unpushed SQL files
            List<File> unPushedFiles = MigrationFileReader.findMigrations(dir, lastDbVersion);

            if (unPushedFiles.isEmpty()) {
                logger.info("No new migrations to apply.");
                return;
            }

            logger.info("Truing to acquireLock");
            while(MigrationLock.acquireLock()){
                logger.info("DB is locked, please wait so it would ba accessible");
                Thread.sleep(30000);
            }
            // Migrate each file
            for (File file : unPushedFiles) {
                String scriptName = file.getName();
                logger.info("Applying migration: {}", scriptName);

                try (FileInputStream in = new FileInputStream(file)) {
                    boolean success = MigrationExecutor.executeSQL(conn, in, scriptName);
                    if (success) {
                        logger.info("Migration applied successfully: {}", scriptName);
                    } else {
                        logger.error("Migration failed: {}", scriptName);
                        break; // Stop on failure
                    }
                } catch (Exception e) {
                    logger.error("Failed to apply migration: {}", scriptName, e);
                    break; // Stop on failure
                }
            }
            MigrationLock.releaseLock();
            logger.info("Migration process completed.");
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }

    private static String getLastDbVersion(Connection conn) {
        //last succesfuly pushed migration
        String query = "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getString("version");
            }
        } catch (Exception e) {
            logger.error("Error fetching last version: {}", e.getMessage(), e);
        }

        return "0"; // Default if no migrations have been applied
    }

    private static void _init_() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS flyway_schema_history (" +
                "installed_rank INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "version VARCHAR(50), " +
                "description VARCHAR(200) NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "script VARCHAR(1000) NOT NULL, " +
                "execution_time INTEGER NOT NULL, " +
                "success BOOLEAN NOT NULL, " +
                "installed_by VARCHAR(100) NOT NULL, " +
                "installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = ConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            //log
            System.out.println("Table 'flyway_schema_history' created or already exists.");

        } catch (SQLException e) {
            //log
            System.err.println("Error creating 'flyway_schema_history': " + e.getMessage());
        }
    }
}
