package org.example;

import org.sqlite.SQLiteException;

import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;

public class MigrationExecutor {
    public static boolean executeSQL(Connection conn, InputStream in, String scriptName) throws SQLException {
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
                        //logs
                        System.out.println("sql query is not good");
                        System.out.println(ex.getResultCode());
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
            logExecution(conn, extractVersionFromFileName(scriptName), scriptName, executionTime, success);
        }
        return success;
    }

    private static String extractVersionFromFileName(String scriptName) {
        // Match the version pattern
        String[] parts = scriptName.split("__", 2);
        if (parts.length > 0) {
            String versionPart = parts[0];
            if (versionPart.startsWith("V") || versionPart.startsWith("B")) {
                return versionPart.substring(1); // Remove the prefix (V or B)
            }
        }
        return null; // Invalid file name format
    }
    private static void createDatabase() {
        String url = PropertiesUtils.getDbUrl();

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("A new database has been created at: ");
            }
        } catch (SQLException e) {
            System.err.println("Error creating database: " + e.getMessage());
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

            //log
            System.out.println("Execution logged for script: " + scriptName);
        } catch (SQLException e) {
            //log
            System.err.println("Error logging execution: " + e.getMessage());
        }
    }



    public static void clean() {
        String url = PropertiesUtils.getDbUrl();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to the database. Starting cleanup...");

            // Fetch all table names in the database
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';"
            );

            // Drop each table
            while (rs.next()) {
                String tableName = rs.getString("name");
                System.out.println("Dropping table: " + tableName);
                stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
            }

            System.out.println("All tables dropped successfully, including 'flyway_schema_history' if present.");

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
