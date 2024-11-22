package org.example.now;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.example.now.COnection.*;

public class MigrationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class); // Logger instance

    public static void migrateFiles(String dir) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_PATH)) {
            logger.info("Connected to the database.");

            // Initialize the database schema history table if it doesn't exist
            _init_();

            File folder = new File(dir);
            if (!folder.exists() || !folder.isDirectory()) {
                logger.error("Invalid directory: {}", dir);
                return;
            }

            // Find the latest version in the database
            String lastDbVersion = getLastDbVersion(conn);

            // Get a list of unpushed SQL files
            List<File> unPushedFiles = MigrationFileReader.findMigrations(dir, lastDbVersion);

            if (unPushedFiles.isEmpty()) {
                logger.info("No new migrations to apply.");
                return;
            }

            // Migrate each file
            for (File file : unPushedFiles) {
                String scriptName = file.getName();
                logger.info("Applying migration: {}", scriptName);

                try (FileInputStream in = new FileInputStream(file)) {
                    boolean success = importSQL(conn, in, scriptName);
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

            logger.info("Migration process completed.");
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }

    public static String getLastDbVersion(Connection conn) {
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
}
