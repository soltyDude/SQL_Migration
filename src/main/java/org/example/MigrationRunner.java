package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.example.COnection.*;

public class MigrationRunner {

    /**
     * Migrates all unpushed SQL files in the given directory, one by one, in the correct version order.
     *
     * @param dir The directory containing the migration SQL files.
     */
    public static void migrateFiles(String dir) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_PATH)) {
            System.out.println("Connected to the database.");

            // Initialize the database schema history table if it doesn't exist
            _init_();

            File folder = new File(dir);
            //papku nachodit
            //papka ta i ne pustaya
            File[] files = folder.listFiles((f, name) -> name.endsWith(".sql"));
            //ther is sql files

            // Find the latest version in the database
            String lastDbVersion = getLastDbVersion(conn);

            // Get a list of unpushed SQL files
            List<File> unPushedFiles = MigrationFileReader.findUnPushed(dir, lastDbVersion);


            if (unPushedFiles.isEmpty()) {
                System.out.println("No new migrations to apply.");
                return;
            }
            boolean success;
            // Migrate each file
            for (File file : unPushedFiles) {
                String scriptName = file.getName();
                System.out.println("Applying migration: " + scriptName);

                try (FileInputStream in = new FileInputStream(file)) {
                    success = importSQL(conn, in, scriptName);
                    if (success) {
                        System.out.println("Migration applied successfully: " + scriptName);
                    }else{
                        System.out.println("oooooooops an eror");
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to apply migration: " + scriptName);
                    e.printStackTrace();
                    break; // Stop on failure
                }
            }

            System.out.println("Migration process completed.");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetches the last applied version from the flyway_schema_history table.
     *
     * @param conn The database connection.
     * @return The last version applied, or "0" if no migrations have been applied.
     */
    private static String getLastDbVersion(Connection conn) {
        String query = "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank DESC LIMIT 1";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getString("version");
            }
        } catch (Exception e) {
            System.err.println("Error fetching last version: " + e.getMessage());
        }

        return "0"; // Default if no migrations have been applied
    }
}
