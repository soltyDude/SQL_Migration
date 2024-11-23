package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteException;

import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;

public class MigrationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

    public static boolean executeSQL(Connection conn, InputStream in, String scriptName, char option) throws SQLException {
        Scanner s = new Scanner(in);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        Statement st = null;
        boolean success = true;
        long startTime = System.currentTimeMillis();
        int executionTime;

        try {
            st = conn.createStatement();
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0) {
                    try {
                        st.execute(line);
                    } catch (SQLiteException ex) {
                        logger.error("SQL query is not valid. Error code: {}", ex.getResultCode(), ex);
                        success = false;
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            success = false;
            throw e;
        } finally {
            executionTime = (int) (System.currentTimeMillis() - startTime);
            if (st != null) st.close();
            if(option == 'V') {
                logExecution(conn, extractVersionFromFileName(scriptName), scriptName, executionTime, success);
            } else if (option == 'R') {
                deleteVersion(conn, extractVersionFromFileName(scriptName));
            }
        }
        return success;
    }

    private static String extractVersionFromFileName(String scriptName) {
        String[] parts = scriptName.split("__", 2);
        if (parts.length > 0) {
            String versionPart = parts[0];
            if (versionPart.startsWith("V") || versionPart.startsWith("R")) {
                return versionPart.substring(1); // Remove the prefix (V or B)
            }
        }
        return null; // Invalid file name format
    }

    private static void createDatabase() {
        String url = PropertiesUtils.getDbUrl();

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                logger.info("A new database has been created at URL: {}", url);
            }
        } catch (SQLException e) {
            logger.error("Error creating database: {}", e.getMessage(), e);
        }
    }

    private static void logExecution(Connection conn, String version, String scriptName, int executionTime, boolean success) {
        String logSQL = "INSERT INTO flyway_schema_history (" +
                "version, description, type, script, execution_time, success, installed_by" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(logSQL)) {
            pstmt.setString(1, version); // version
            pstmt.setString(2, "Executed script: " + scriptName); // description
            pstmt.setString(3, "SQL"); // type
            pstmt.setString(4, scriptName); // script
            pstmt.setInt(5, executionTime); // execution_time
            pstmt.setBoolean(6, success); // success
            pstmt.setString(7, System.getProperty("user.name")); // installed_by (current user)
            pstmt.executeUpdate();

            logger.info("Execution logged for script: {}", scriptName);
        } catch (SQLException e) {
            logger.error("Error logging execution for script {}: {}", scriptName, e.getMessage(), e);
        }
    }

    private static void deleteVersion(Connection conn, String version) {
        String deleteSQL = "DELETE FROM flyway_schema_history WHERE version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setString(1, version); // Set the specific version to delete
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                logger.info("Successfully deleted version: {}", version);
            } else {
                logger.warn("No entry found for version: {}", version);
            }
        } catch (SQLException e) {
            logger.error("Error deleting version {}: {}", version, e.getMessage(), e);
        }
    }

    public static void clean() {
        String url = PropertiesUtils.getDbUrl();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            logger.info("Connected to the database. Starting cleanup...");

            // Fetch all table names in the database
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"
            );

            // Drop each table
            while (rs.next()) {
                String tableName = rs.getString("name");
                logger.info("Dropping table: {}", tableName);
                stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
            }

            logger.info("All tables dropped successfully, including 'flyway_schema_history' if present.");

        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
}
