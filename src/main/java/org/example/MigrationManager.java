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


    /**
     * Applies all new migration files found in the specified directory.
     *
     * @param dir the directory containing migration files
     */
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

            // Find the latest version in the database

            String lastDbVersion = getLastDbVersion(conn);

            // Get a list of unpushed SQL files
            List<File> unPushedFiles = MigrationFileReader.findMigrations(dir, lastDbVersion, 'V');

            if (unPushedFiles.isEmpty()) {
                logger.info("No new migrations to apply.");
                return;
            }

            logger.info("Truing to acquireLock");
            while(!MigrationLock.acquireLock()){
                logger.info("DB is locked, please wait so it would ba accessible");
                Thread.sleep(30000);
            }
            // Migrate each file
            try {
                conn.setAutoCommit(false); // Start transaction

                for (File file : unPushedFiles) {
                    String scriptName = file.getName();
                    logger.info("Applying migration: {}", scriptName);

                    try (FileInputStream in = new FileInputStream(file)) {
                        boolean success = MigrationExecutor.executeSQL(conn, in, scriptName, 'V');
                        if (success) {
                            logger.info("Migration applied successfully: {}", scriptName);
                        } else {
                            throw new Exception("Migration failed: " + scriptName);
                        }
                    }
                }

                conn.commit(); // Commit all migrations if everything succeeds
                logger.info("All migrations applied successfully.");

            } catch (Exception e) {
                try {
                    conn.rollback(); // Rollback if any migration fails
                    logger.error("Migration process failed. All changes have been rolled back.", e);
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction.", rollbackEx);
                }
            } finally {
                try {
                    conn.setAutoCommit(true); // Restore default auto-commit behavior
                } catch (SQLException autoCommitEx) {
                    logger.error("Failed to reset auto-commit.", autoCommitEx);
                }
            }
            MigrationLock.releaseLock();
            logger.info("Migration process completed.");
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Applies all new migrations found in the default directory (`src/main/resources`).
     */
    public static void migrateFiles(){
        migrateFiles("src/main/resources");
    }


    /**
     * Applies rollback files to revert migrations up to the specified version.
     *
     * @param dir     the directory containing rollback files
     * @param vertion the target version to rollback to
     */
    public static void migrateRollbacks(String dir, String vertion) {
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

            // Find the latest version in the database


            // Get a list of unpushed SQL files
            List<File> unPushedFiles = MigrationFileReader.findMigrations(dir, vertion, 'R');

            if (unPushedFiles.isEmpty()) {
                logger.info("No new migrations to apply.");
                return;
            }

            logger.info("Truing to acquireLock");
            while(!MigrationLock.acquireLock()){
                logger.info("DB is locked, please wait so it would ba accessible");
                Thread.sleep(30000);
            }
            // Migrate each file
            try {
                conn.setAutoCommit(false); // Start transaction

                for (int i = unPushedFiles.size() - 1; i >= 0; i--) {
                    File file = unPushedFiles.get(i);

                    String scriptName = file.getName();
                    logger.info("Applying migration: {}", scriptName);

                    try (FileInputStream in = new FileInputStream(file)) {
                        boolean success = MigrationExecutor.executeSQL(conn, in, scriptName, 'R');
                        if (success) {
                            logger.info("Migration applied successfully: {}", scriptName);
                        } else {
                            throw new Exception("Migration failed: " + scriptName);
                        }
                    }
                }

                conn.commit(); // Commit all migrations if everything succeeds
                logger.info("All migrations applied successfully.");

            } catch (Exception e) {
                try {
                    conn.rollback(); // Rollback if any migration fails
                    logger.error("Migration process failed. All changes have been rolled back.", e);
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction.", rollbackEx);
                }
            } finally {
                try {
                    conn.setAutoCommit(true); // Restore default auto-commit behavior
                } catch (SQLException autoCommitEx) {
                    logger.error("Failed to reset auto-commit.", autoCommitEx);
                }
            }
            MigrationLock.releaseLock();
            logger.info("Migration process completed.");
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Applies rollbacks using files in the default directory (`src/main/resources`).
     *
     * @param vertion the target version to rollback to
     */
    public static void migrateRollbacks(String vertion){
        migrateRollbacks("src/main/resources", vertion);
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
            logger.info("Table 'flyway_schema_history' created or already exists.");

        } catch (SQLException e) {
            //log
            logger.error("Error creating 'flyway_schema_history': " + e.getMessage());
        }
    }
}
