package org.example.now;

import org.sqlite.SQLiteException;

import java.io.*;
import java.sql.*;
import java.util.Scanner;

public class COnection {
    private boolean succesful = true;
    public static final String DB_FILE_PATH = "data/mydatabase.db";
    public static final String SQL_FILE_PATH = "data/V0_0_01__aboba.sql";

    /*public static void ensureDatabaseExists() {
        File dbFile = new File(DB_FILE_PATH);

        if (dbFile.exists()) {
            System.out.println("Database already exists: " + DB_FILE_PATH);
        } else {
            System.out.println("Database not found. Creating a new database...");
            createDatabase();
        }
    }*///ensure db exists

    private static void createDatabase() {
        String url = "jdbc:sqlite:" + DB_FILE_PATH;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("A new database has been created at: " + DB_FILE_PATH);
            }
        } catch (SQLException e) {
            System.err.println("Error creating database: " + e.getMessage());
        }
    }

    //надо чтобы он откатывал первый стэйтмент если на втором ошибка или не исполнял его
    //возможно чтобы он весь файл исполнял одной кверей
    public static boolean executeSQL(Connection conn, InputStream in, String scriptName) throws SQLException {
        /*
        String version = extractVersionFromFileName(scriptName);
        if (version == null) {
            System.err.println("Invalid script name format. Skipping: " + scriptName);
            return false;
        }
        //не нужно
        if (isVersionAlreadyApplied(conn, version)) {
            System.out.println("Version " + version + " already applied. Skipping: " + scriptName);
            return false;
        }
        */
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
                        System.out.println("sql query is not good");
                        System.out.println(ex.getResultCode());
                        //чтобы он поставил фолс
                        //после ошибки брэйк
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

    //не нужно
    /*
    private static boolean isVersionAlreadyApplied(Connection conn, String version) throws SQLException {
        // Query to check if the version exists and was successfully applied
        String selectCOUNT = "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = TRUE";

        try (PreparedStatement pstmt = conn.prepareStatement(selectCOUNT)) {
            pstmt.setString(1, version);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0; // Returns true if the version exists and was successful
            }
        }
    }
*/
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

            System.out.println("Execution logged for script: " + scriptName);
        } catch (SQLException e) {
            System.err.println("Error logging execution: " + e.getMessage());
        }
    }

    public static void _init_() {
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

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE_PATH);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSQL);
            System.out.println("Table 'flyway_schema_history' created or already exists.");

        } catch (SQLException e) {
            System.err.println("Error creating 'flyway_schema_history': " + e.getMessage());
        }
    }

        /**
         * Wipes out all data in the database, including the flyway_schema_history table.
         */
        public static void clean() {
            String url = "jdbc:sqlite:" + DB_FILE_PATH;

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

